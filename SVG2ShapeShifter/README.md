
# SVG2ShapeShifter

SVG2ShapeShifter is a command-line tool that converts SVG and animated SVG files created by the 3D modeling application [[http://blender.org][Blender]] to [[https://alexjlockwood.github.io/ShapeShifter/][Shape Shifter]] format.

## Problem

Creating animations isn't the simplest endeavour. In the mobile space, several methods have appeared as options.

- Developer re-implements / hand codes what the designer has given them
- Developer uses Shape Shifter to design the animation
- Designer adds [[https://airbnb.design/lottie/][Lottie]] to their workflow and developer uses the generated assets

No one should code AVDs so let's remove that as non-viable. Lottie, requiring After Effects which doesn't run on Linux, was also knocked out of my choices.

Shape Shifter is great in simplifying the steps to get to a working animation for the masses. However, creating complex drawings isn't its strong suit. Over time as I explored it, I wanted more and more to design in other tools and only animate them in ShapeShifter. Blender, which runs on all platforms and has a feature set superceding After Effects, was my choice of design tool.

Blender speaks SVG and so does ShapeShifter so you can create in Blender and export to SVG, easy peasy. Blender's SVG is slightly incompatible so problem one was converting Blender's SVG to an easier format. Though not documented, I found native Shape Shifter easy to write a generator for.

With that solved, it cut down my time needed manually create paths in Shape Shifter. I only needed to animate them. [[https://medium.com/@ecspike/creating-animatedvectordrawables-with-shape-shifter-part-ii-4142ba1ad74b][A simple eye blink animation]] took 10 timeline events to create in Shape Shifter. Doable but tedious as animations grow more complex. That exposed problem two. The number of timeline events needed to create something manually in Shape Shifter often increases greater than linear scale and pretty hard to adjust if one change had to cascade. The second major revision was parsing an animated SVG from Blender into ShapeShifter format.


## Examples
[[file:art/blender-hello-1280.gif]]

[[file:art/shapeshifter-hello-1280.gif]]


## How does it work ?

Blender's FreeStyle plugin renders using Z-depth, 3D mesh data, and other variables to draw lines and edges. It can simulate various cartoon styles as well as technical blue print illustration. That line and edge data for all meshes and transformations is converted to path data as a lowest common denominator and exported to SVG. Color data is handled as well.

Blender exports a list of paths it sees to SVG for every frame in the animation with no relationship between one frame and another. SVG2ShapeShifter reads the frame data and attempts to create a morph timeline event between the source frame and the target frame.

## Supported Blender features

- Blender primitive meshes
- Bezier curves
- Metaballs
- Text (but not a good idea)
- Color animation
- Rotation, translation, scaling
- Blender keyframes
- Shape keys

## Caveats

This is very experimental. Moving the camera in your scene can cause some paths to appear or disappear between frames and cause some paths to not be animated. Some paths will still need to be autofixed in Shape Shifter. Some autofixed paths get rotated weirdly.

## Requirements

- Java 8
- Blender. Follow this [[https://][guide]] to install and setup the required plugins for export.


## Usage

Usage:

`SVG2ShapeShifter<.bat/sh> <options>`

The dist/ folder has a current build that can be useful for testing.

You can run it directly from the command line with this command:

`java -jar dist\SVG2ShapeShifter-1.0-SNAPSHOT.jar -f examples\0001-0030.svg`

It should output something like this:

```
Starting SVG to .shapeshifter conversion...
Converting file: examples\0001-0030.svg
Write to converted file: 0001-0030.shapeshifter
Fixing animation timeline duration: 20
Fixed block size: 20
Completed SVG to .shapeshifter.
```

The 0001-0030.shapeshifter file can be opened in the Shape Shifter website here: [[https://shapeshifter.design/]]

| Property                     | Description                               |
| -f,--file <arg>              | File to convert                           |
| -frame,--frameInterval <arg> | Sets the number of frames to skip between |
|                              | keyframes when exporting.                 |
|                              | Default: 5                                |
| -h,--help                    | Print this help message                   |
| -time,--timeInterval <arg>   | Sets the time interval between keyframes  |
|                              | for animations [in milliseconds]          |
|                              | Default: 50                               |

Output filename is always the input filename with a ~.shapeshifter~ extension.
