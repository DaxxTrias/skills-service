/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <button class="icon-button"
       @click="selectIcon"
       @keypress.enter="selectIcon"
       role="button"
       aria-roledescription="icon selector button"
       aria-label="icon selector"
       tabindex="0"
       :disabled="disabled"
       data-cy="iconPicker">
    <div class="card-body text-primary" style="min-height: 4rem;">
      <i :class="[selectedIconClass]" />
    </div>
  </button>
</template>

<script>
  export default {
    name: 'IconPicker',
    props: {
      startIcon: String,
      customIconHeight: {
        type: Number,
        default: 48,
      },
      customIconWidth: {
        type: Number,
        default: 48,
      },
      disabled: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        hideAvailableIcons: true,
        selectedIconClass: this.startIcon,
      };
    },
    methods: {
      selectIcon() {
        this.$emit('select-icon');
      },
      onSelectedIcon(selectedIcon) {
        this.selectedIconClass = `${selectedIcon.css}`;
        this.hideAvailableIcons = true;
        this.$emit('on-icon-selected', this.selectedIconClass);
        this.$bvModal.hide('icons');
      },
      close() {
        this.hideAvailableIcons = true;
      },
    },
  };
</script>

<style scoped>
  i {
    font-size: 3rem;
  }

  .icon-button {
    border: 1px solid rgba(0, 0, 0, 0.125);
    border-radius: 0.25em;
    background-color: #fff;
  }

  .icon-button:disabled {
    background-color: lightgrey;
    cursor: none;
  }
</style>
