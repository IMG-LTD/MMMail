<script setup lang="ts">
import { NTag } from "naive-ui";
import { useLocaleText } from "@/locales";
import type { TextLike, Tone } from "../types";

const props = withDefaults(
  defineProps<{
    tone: Tone;
    label: TextLike;
    icon?: string;
  }>(),
  {
    icon: undefined,
  },
);

const { tr } = useLocaleText();

function getTagType(tone: Tone) {
  if (tone === "danger") {
    return "error";
  }

  if (tone === "brand") {
    return "primary";
  }

  return tone === "neutral" ? "default" : tone;
}
</script>

<template>
  <NTag :type="getTagType(tone)">
    <span v-if="props.icon" aria-hidden="true">{{ props.icon }}</span>
    {{ tr(label) }}
  </NTag>
</template>
