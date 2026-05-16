import { defineConfig } from '@soybeanjs/eslint-config-vue';

const config = await defineConfig({
  'vue/component-name-in-template-casing': [
    'warn',
    'PascalCase',
    {
      registeredComponentsOnly: false,
      ignores: ['/^icon-/']
    }
  ]
});

export default [{ ignores: ['coverage/**', 'dist/**'] }, ...config];
