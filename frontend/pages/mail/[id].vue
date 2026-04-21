<script setup lang="ts">
const route = useRoute()

await navigateTo(`/conversations/${String(route.params.id || '')}`)
</script>
