import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
import ButtonIcon from '@/components/custom/button-icon.vue';

const global = {
  stubs: {
    NButton: {
      template: '<button class="n-button" :class="$attrs.class"><slot /></button>'
    },
    NTooltip: {
      props: ['disabled', 'placement', 'zIndex'],
      template:
        '<div class="n-tooltip" :data-disabled="String(disabled)" :data-placement="placement" :data-z-index="String(zIndex)"><slot name="trigger" /><span class="tooltip-content"><slot /></span></div>'
    },
    SvgIcon: {
      props: ['icon'],
      template: '<i class="svg-icon" :data-icon="icon" />'
    }
  }
};

describe('ButtonIcon', () => {
  it('renders a tooltip-backed icon button', () => {
    const wrapper = mount(ButtonIcon, {
      global,
      props: {
        icon: 'mdi:home',
        tooltipContent: 'Open home'
      }
    });

    expect(wrapper.find('.n-tooltip').attributes('data-disabled')).toBe('false');
    expect(wrapper.find('.n-tooltip').attributes('data-placement')).toBe('bottom');
    expect(wrapper.find('.n-tooltip').attributes('data-z-index')).toBe('98');
    expect(wrapper.find('.svg-icon').attributes('data-icon')).toBe('mdi:home');
    expect(wrapper.find('.tooltip-content').text()).toBe('Open home');
  });

  it('merges custom classes and supports a custom slot', () => {
    const wrapper = mount(ButtonIcon, {
      global,
      props: {
        class: 'text-primary',
        tooltipPlacement: 'top',
        zIndex: 120
      },
      slots: {
        default: '<span class="custom-content">Save</span>'
      }
    });

    expect(wrapper.find('.n-button').classes()).toContain('text-primary');
    expect(wrapper.find('.custom-content').text()).toBe('Save');
    expect(wrapper.find('.n-tooltip').attributes('data-disabled')).toBe('true');
    expect(wrapper.find('.n-tooltip').attributes('data-placement')).toBe('top');
    expect(wrapper.find('.n-tooltip').attributes('data-z-index')).toBe('120');
  });
});
