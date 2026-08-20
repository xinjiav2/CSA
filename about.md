---
layout: page
title: About
permalink: /about/
---

Hi! I'm Zhengji

Where I'm from

<div id="grid_container"></div>

<script>
    (function() {
        // Use the existing container if present, otherwise create and append one
        const outputElement = document.getElementById('grid_container') || (function() {
            const d = document.createElement('div');
            d.id = 'grid_container';
            document.body.appendChild(d);
            return d;
        })();

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
        // Use the existing output element as the container
        const container = outputElement;

        // Style the container
        container.style.border = '2px solid';
        container.style.padding = '10px';
        container.style.display = 'grid';
        container.style.gridTemplateColumns = 'repeat(auto-fill, minmax(150px, 1fr))';
        container.style.gap = '10px';

        // Loop through data and create grid items
        for (const location of living_in_the_world) {
            const gridItem = document.createElement('div');
            gridItem.style.textAlign = 'center';

            const img = document.createElement('img');
            img.src = location.flag;
            img.alt = location.description + ' Flag';
            img.style.width = '100%';
            img.style.height = '100px';
            img.style.objectFit = 'contain';

            const description = document.createElement('p');
            description.textContent = location.description;
            description.style.margin = '5px 0';
            description.style.fontWeight = 'bold';

            const greeting = document.createElement('p');
            greeting.textContent = location.greeting;
            greeting.style.margin = '5px 0';
            greeting.style.fontStyle = 'italic';
            greeting.style.opacity = '0.7';

            gridItem.appendChild(img);
            gridItem.appendChild(description);
            gridItem.appendChild(greeting);

            container.appendChild(gridItem);
        }
    })();
</script>

