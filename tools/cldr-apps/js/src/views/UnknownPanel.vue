<template>
  <div id="home">
    <h1>
      Oops! Can’t find the <code>#{{ $specialPage }}</code> page.
    </h1>
    <p>⚠️ Something’s wrong and we can’t find that page.</p>
    <p class="yeah">This is probably a bug.</p>
    <!-- Report a bug here? -->
  </div>
</template>

<script>
export default {};
</script>

<style scoped>
.yeah {
  font-family: fantasy;
}
</style>
