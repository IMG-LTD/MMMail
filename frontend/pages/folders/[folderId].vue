<script setup lang="ts">
const route = useRoute()

await navigateTo(`/folders/${String(route.params.folderId || '')}`)
</script>
