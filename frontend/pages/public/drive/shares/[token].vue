<script setup lang="ts">
const route = useRoute()

await navigateTo(`/share/drive/${String(route.params.token || '')}`)
</script>
