---
layout: page
title: About
permalink: /about/
---

Hi! I'm Zhengji

<div id="grid_container"></div>

<script>
    // Clear the output
outputElement.innerHTML = '';

// Data array
const living_in_the_world = [
  {
    flag: "https://upload.wikimedia.org/wikipedia/commons/thumb/f/fa/Flag_of_the_People%27s_Republic_of_China.svg/1920px-Flag_of_the_People%27s_Republic_of_China.svg.png?utm_source=en.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
    greeting: "shanghai ",
    description: "china"
  },
  {
    flag: "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d9/Flag_of_Canada_%28Pantone%29.svg/3840px-Flag_of_Canada_%28Pantone%29.svg.png?utm_source=en.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
    greeting: "vancouver",
    description: "canada"
  },
  {
    flag: "https://upload.wikimedia.org/wikipedia/commons/thumb/4/48/Flag_of_Singapore.svg/1920px-Flag_of_Singapore.svg.png?utm_source=en.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
    greeting: "singapore",
    description: "singapore"
  },
  {
    flag: "https://upload.wikimedia.org/wikipedia/commons/thumb/9/96/Flag_of_the_United_States_%28DDD-F-416E_specifications%29.svg/3840px-Flag_of_the_United_States_%28DDD-F-416E_specifications%29.svg.png?utm_source=en.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
    greeting: "san diego",
    description: "america"
  }
];

// Create a div container with id
const container = document.createElement('div');
container.id = 'grid_container';

// Style the container 
container.style.border = '2px solid';
container.style.padding = '10px';

// Grid specific styles
container.style.display = 'grid';
container.style.gridTemplateColumns = 'repeat(auto-fill, minmax(150px, 1fr))';
container.style.gap = '10px';

// Loop through data and create grid items
for (const location of living_in_the_world) {
  // Create grid item
  const gridItem = document.createElement('div');
  gridItem.style.textAlign = 'center';
  
  // Create a flag image
  const img = document.createElement('img');
  img.src = location.flag;
  img.alt = location.description + ' Flag';
  img.style.width = '100%';
  img.style.height = '100px';
  img.style.objectFit = 'contain';
  
  // Create a description
  const description = document.createElement('p');
  description.textContent = location.description;
  description.style.margin = '5px 0';
  description.style.fontWeight = 'bold';
  
  // Create a greeting
  const greeting = document.createElement('p');
  greeting.textContent = location.greeting;
  greeting.style.margin = '5px 0';
  greeting.style.fontStyle = 'italic';
  greeting.style.opacity = '0.7';
  
  // Add all elements to grid item
  gridItem.appendChild(img);
  gridItem.appendChild(description);
  gridItem.appendChild(greeting);
  
  // Add grid item to container
  container.appendChild(gridItem);
}

// Add containter to output 
outputElement.appendChild(container);

</script>

