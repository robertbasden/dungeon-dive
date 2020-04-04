# dungeon-dive

## Playing

You can play this game [here](https://loving-morse-ada063.netlify.com/)

### Basics

- Use the arrow keys to move / attack
- Make it to the stairs to go to the next level

## Developing

- Clone the repository
- `lein fig:build` to setup a development server
- Edit files and changes should show (c/o [figwheel](https://github.com/bhauman/figwheel-main))

## Resources

### Grafics

- https://www.kenney.nl/assets/bit-pack
- https://www.kenney.nl/assets/ui-pack-rpg-expansion

### Development

- https://gamedevelopment.tutsplus.com/tutorials/how-to-use-tile-bitmasking-to-auto-tile-your-level-layouts--cms-25673
- http://roguebasin.com/index.php?title=Main_Page

## Todo

- OffscreenCanvas doesn't work in Firefox, needs replacing
- BSP doesn't generate very interesting segments
- Seeded random number generation (https://stackoverflow.com/questions/26073686/how-to-generate-identical-series-of-pseudo-random-integers)
- Map size has to be 30x30 ... not sure why...
