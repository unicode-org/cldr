// sidebar-subsections.js
// Automatically populates and highlights in-page subsections under the active chapter in mdBook's sidebar.

(function () {
  'use strict';

  function initSubsections() {
    var sidebar = document.querySelector('#sidebar');
    if (!sidebar) return;

    var activeLink = sidebar.querySelector('.active') || sidebar.querySelector('a[href$="' + window.location.pathname.split('/').pop() + '"]');
    if (!activeLink) return;

    var parentLi = activeLink.closest('.chapter-item') || activeLink.parentElement;
    if (!parentLi) return;

    // Avoid duplicate injection
    if (parentLi.querySelector('.subsection-list')) return;

    var content = document.querySelector('.content main') || document.querySelector('main');
    if (!content) return;

    // Collect all major headings (h2, h3) that have an id or anchor
    var headings = content.querySelectorAll('h2, h3');
    if (!headings || headings.length === 0) return;

    var subList = document.createElement('ol');
    subList.className = 'section subsection-list';
    subList.setAttribute('aria-label', 'Subsections');

    var count = 0;
    headings.forEach(function (h) {
      // Find or generate an ID
      var id = h.id;
      if (!id) {
        var anchor = h.querySelector('a[id], a[name]');
        if (anchor) {
          id = anchor.id || anchor.getAttribute('name');
        }
      }
      if (!id) {
        var text = h.textContent.trim().toLowerCase().replace(/[^a-z0-9_-]+/g, '-');
        id = text;
        h.id = id;
      }

      var titleText = h.textContent.trim();
      // Skip empty or purely decorative headings
      if (!titleText || titleText.toLowerCase() === 'parts' || titleText.toLowerCase() === 'contents') return;

      var li = document.createElement('li');
      li.className = 'chapter-item subsection-item' + (h.tagName.toLowerCase() === 'h3' ? ' subsection-h3' : ' subsection-h2');

      var a = document.createElement('a');
      a.href = '#' + id;
      a.textContent = titleText;
      a.className = 'subsection-link';

      a.addEventListener('click', function (e) {
        var target = document.getElementById(id);
        if (target) {
          e.preventDefault();
          target.scrollIntoView({ behavior: 'smooth', block: 'start' });
          history.pushState(null, '', '#' + id);
          highlightActiveSub(a);
        }
      });

      li.appendChild(a);
      subList.appendChild(li);
      count++;
    });

    if (count > 0) {
      parentLi.appendChild(subList);
      parentLi.classList.add('has-subsections', 'expanded');
    }

    // ScrollSpy: highlight active subsection on scroll
    var subLinks = subList.querySelectorAll('.subsection-link');
    function highlightActiveSub(activeAnchor) {
      subLinks.forEach(function (link) {
        link.classList.remove('active-sub');
      });
      if (activeAnchor) {
        activeAnchor.classList.add('active-sub');
      }
    }

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            var matchingLink = subList.querySelector('a[href="#' + entry.target.id + '"]');
            if (matchingLink) {
              highlightActiveSub(matchingLink);
            }
          }
        });
      },
      { rootMargin: '-20% 0px -70% 0px' }
    );

    headings.forEach(function (h) {
      if (h.id) observer.observe(h);
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initSubsections);
  } else {
    // Retry slightly after to ensure mdbook-sidebar-scrollbox custom element has rendered
    setTimeout(initSubsections, 50);
    setTimeout(initSubsections, 250);
  }
})();
