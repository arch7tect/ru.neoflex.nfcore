import { configure } from '@storybook/react';
import '../css/style.css';
import '../css/icons.css';

function loadStories() {
  require('../stories/General/');
  require('../stories/Feedback/');
  // You can require as many stories as you need.
}

configure(loadStories, module);
