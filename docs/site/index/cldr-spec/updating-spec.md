---
title: Updating the Spec
---

This document discusses processes, procedures, and practices for the update of [UTS #35, the LDML Spec](/index/cldr-spec)

## General Information

### Hosting

The latest spec is hosted at <https://unicode.org/reports/tr35>. Various other versions are available, such as per-CLDR release <https://unicode.org/reports/tr35/44/> or a specific revision number, <https://www.unicode.org/reports/tr35/tr35-70/tr35.html>

The well-known URL <https://www.unicode.org/reports/tr35/proposed.html> redirects to <https://www.unicode.org/reports/tr35/dev/>. To move `dev`, the symlink has to be modified on the server.  You can see which revision it goes to by the "This Version" URL at that location.

The latest spec is also controlled by a symlink on the server, named `tr35-CURRENT`.

Preview URLs for the spec are hosted via cloudflare, the `cldr-spec` project.

The sections below discuss the updating and deployment process.

### Proposing Changes

Changes to the site are effected by means of GitHub pull requests. When a PR is created that updates [`docs/ldml/*.md`][docs/ldml], a special label is automatically added, [*Spec Update*](https://github.com/unicode-org/cldr/labels/Spec%20Update)

If the branch is on the "head fork" (the [CLDR upstream repository][unicode-org/cldr]), then a preview link is generated for each commit.

> This is the recommended practice, so that the spec can be previewed exactly as it will be deployed. Create a branch with your GitHub username, such as `srl295/CLDR-1234`   This is different from how we normally create CLDR feature branches in a personal fork. PRs that are NOT on the head fork will be harder to review because we can’t preview them. Review such changes carefully.

The preview links are comments on the pull request, for example *Spec update preview at <https://efb876f0-cldr-spec.unicode.workers.dev>*. This *commit preview URL* shows what each specific commit looks like.

## Authoring Spec Content

We follow Github Flavored Markdown plus some extensions. The [Github Guide](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/basic-writing-and-formatting-syntax) can provide helpful guidance on the format.

The spec is divided into parts, with `tr35.md` being part 1 (Core), plus appendices such as `tr35-modifications.md`

### Links

Each heading automatically produces a link anchor. You can mouse over, or click, the link symbol that appears next to an anchor to determine its short anchor. For example, the markdown `### DTD Annotations` automatically produces the link `#dtd-annotations`.

To link between parts of the spec, **always use the .md file name**, for example `tr35-collation.md#Rules`.  Never use the rendered `.html` filename.

Headings must remain stable. Avoid removing or renaming headings. If they must be renamed, add an `<a name="Old_Name"/>` element prior to the heading, so that the Old_Name can still be referenced.

You will see `<a href` used throughout the spec, these are historical links. New links should be added in Markdown format.

## Techniques and Procedures

### Version and Status

The frontmatter at the top of `tr35.md` controls the draft status of the entire spec. Use `status: draft` or `status: final` appropriately. Set the `version` to the CLDR version under development, and the `revision` to the unique revision number of the spec.


```yaml
---
version: 49
status: draft
revision: 79
---
```

### Testing Locally

See <https://github.com/unicode-org/cldr/blob/main/tools/scripts/tr-archive/README.md> for details on how to render the spec on your local system.

### Approving Deployment

Merging to main of a PR that updates the spec, will trigger a preview and an attempted production deployment.

1. [this link](https://github.com/unicode-org/cldr/actions/workflows/spec.yml?query=branch%3Amain) will take privileged TC members to the most recent attempted deployment of the spec (as merged to main).

2. If the most recent run is in state "waiting" (showing a clock), click on that run.

3. In the build summary, there will be an entry near the bottom with the *preview* deployment such as:

    > Spec update tr35-79 preview at <https://46451717-cldr-spec.unicode.workers.dev>

4. Note the revision number (tr35-79). This means that the spec is attempting to update revision (not version) 79.

5. Click on the preview link, and compare it to the current revision 79 at <https://www.unicode.org/reports/tr35/tr35-79/tr35.html>

6. If you agree with this update to the spec revision, click the Review Deployments button. (TC members may also receive an email with this button.). Check the "spec-production" checkbox, enter a comment if desired ,and choose Approve and Deploy.

7. The build run will continue, and the "Deploy Spec to Production" run will proceed.

[docs/ldml]: https://github.com/unicode-org/cldr/tree/main/docs/ldml
