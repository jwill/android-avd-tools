import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.util.Node
import groovy.util.NodeList
import groovy.util.XmlParser
import groovyjarjarcommonscli.DefaultParser
import groovyjarjarcommonscli.HelpFormatter
import groovyjarjarcommonscli.Options
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

fun main(args: Array<String>) {
    // Call the converter from IDEA
    println("Hello World!")
    ConvertSVG().renderFile("examples/0001-0030.svg")
    // Use from the commandline
    //ConvertSVG(args)
}

class ConvertSVG() {
    var frameInterval = 5
    var timeInterval = 50


    constructor(args: Array<String>) : this() {
        val parser = DefaultParser()
        val options = Options()
        with(options) {
            addOption("h", "help", false, "Print this help message")
            addOption("time", "timeInterval", true, "Sets the time interval between keyframes for animations [in 1/100th of seconds]\nDefault: 20")
            addOption("frame", "frameInterval", true, "Sets the number of frames to skip between keyframes when exporting.\n Default: 5")
            addOption("f", "file", true, "File to convert")
        }

        val cmd = parser.parse(options, args)
        val formatter = HelpFormatter()

        if (cmd.hasOption("time")) {
            val time = cmd.getOptionValue("time").toInt()
            if (time > 0) timeInterval = time
        }
        if (cmd.hasOption("frame")) {
            val frame = cmd.getOptionValue("frame").toInt()
            if (frame > 0) frameInterval = frame
        }

        if (!cmd.hasOption("file")) {
            println("You need to specify a file.")
            formatter.printHelp("SVG2ShapeShifter", options)
        } else {
            renderFile(cmd.getOptionValue("file"))
        }
    }


    fun renderFile(filename: String) {
        // Load SVG generated from Blender
        val file = File(filename)
        val outputFile = File(file.name.removeSuffix(".svg") + ".shapeshifter")

        if (!file.exists()) {
            println("File ${file.name} doesn't exist")
            System.exit(1)
        }

        val svg = XmlParser().parse(file)

        val svgHeight = svg.attribute("height").toString().toInt()
        val svgWidth = svg.attribute("width").toString().toInt()

        // first child is always RenderLayer_LineSet
        val renderLayer_LineSet = (svg.get("g") as NodeList)[0] as Node

        // The value of renderLayer_LineSet is either a NodeList of frames
        // frame_0001, frame_0002, ... frame_NNNN -> fills, strokes
        // or
        // (single frame) fills, strokes
        when (isMultiFrameSVG(renderLayer_LineSet)) {
            true -> {
                val drawingData = loadFrameZeroAndAnimation(renderLayer_LineSet)
                val json = composeAnimation(svgHeight, svgWidth, drawingData)
                outputFile.writeText(JsonOutput.prettyPrint(json))
            }

            false -> {
                val json = loadSingleFrame(svgHeight, svgWidth, renderLayer_LineSet)
                outputFile.writeText(JsonOutput.prettyPrint(json))
            }
        }
    }

    fun isMultiFrameSVG(node: Node): Boolean {
        val frames = (node.value() as NodeList)
        // Node.value will either be a group of fills and a group of paths
        // or keyframe(s)

        // If the frame is

        val keyFrames = frames.filter {
            val n = it as Node
            val attributesMap = n.attributes()
            attributesMap["id"].toString().startsWith("frame_")
        }
        return keyFrames.size > 1
    }

    fun loadFrameZeroAndAnimation(renderLayer_LineSet: Node): HashMap<String, Any> {
        val frames = (renderLayer_LineSet.value() as NodeList)
        val processedFrames = arrayListOf<Map<String, Group>>()

        frames.forEachIndexed { _, node ->
            val fills = processPaths("fills", node as Node)
            val strokes = processPaths("strokes", node as Node)
            processedFrames.add(mapOf(
                    "fills" to fills, "strokes" to strokes
            ))
        }

        var currentTime = 0
        val sanitizeFrameId = {frame:Node ->
            val id = frame.attribute("id").toString()
            val intString:String = id.removePrefix("frame_")
             Integer.parseInt(intString)
        }
        val frameId = sanitizeFrameId(frames[0] as Node)
        if (frameId >= 10) {
            currentTime = frameId * 10
        }

        val blocks = arrayListOf<TimelineBlock>()
        val frame0 = processedFrames[0]
        val frame0Fills = frame0["fills"]
        val frame0Strokes = frame0["strokes"]

        for (index in 0 until processedFrames.size - 1 step frameInterval) {
            val currentFrame = processedFrames[index]
            val currentFrameFills = currentFrame["fills"]
            val currentFrameStrokes = currentFrame["strokes"]

            val nextFrame = processedFrames.getOrNull(index + frameInterval)
            if (nextFrame != null) {
                val nextFrameFills = nextFrame["fills"]
                val nextFrameStrokes = nextFrame["strokes"]

                for (i in 0 until frame0Fills?.children!!.size) {
                    parseGroupForAnimation(frame0Fills, i, currentFrameFills, nextFrameFills, currentTime, blocks)
                }

                for (j in 0 until frame0Strokes?.children!!.size) {
                    parseGroupForAnimation(frame0Strokes, j, currentFrameStrokes, nextFrameStrokes, currentTime, blocks, isFill = false)
                }
                currentTime += timeInterval
            }
        }


        val animation = AnimationTimeline(UUID.randomUUID().toString(),
                blocks = blocks
        )
        animation.fixDuration();

        return hashMapOf("frame0" to arrayListOf(frame0Fills, frame0Strokes),
                "animation" to animation)
    }

    private fun parseGroupForAnimation(frameFillOrStrokes: Group, i: Int,
                                       currentFrameGroup: Group?, nextFrameGroup: Group?,
                                       currentTime: Int, blocks: ArrayList<TimelineBlock>, isFill: Boolean = true) {

        // check for possible color animation block
        var currentColor: String? = ""
        var nextFrameColor: String? = ""

        var currentAlpha: Number? = 1.0
        var nextFrameAlpha: Number? = 1.0
        var colorProperty: TimelineProperty = TimelineProperty.FILL_COLOR
        var alphaProperty: TimelineProperty = TimelineProperty.FILL_ALPHA

        try {
            // Check path data
            val currentPath = (currentFrameGroup!!.children[i] as Path).pathData!!
            val nextPath = (nextFrameGroup!!.children[i] as Path).pathData!!

            if (currentPath != nextPath) {
                val timelineBlock = TimelineBlock(
                        id = UUID.randomUUID().toString().replace("-",""),
                        layerId = (frameFillOrStrokes.children[i] as Path).id,           // never changes
                        type = TimelineType.PATH.id,
                        fromValue = currentPath,   // changes per frame
                        toValue = nextPath,
                        propertyName = TimelineProperty.PATH_DATA.id,

                        startTime = currentTime,
                        endTime = currentTime + timeInterval
                )
                blocks.add(timelineBlock)
            }



            when (isFill) {
                true -> {
                    currentColor = (currentFrameGroup.children[i] as Path).fillColor
                    currentAlpha = (currentFrameGroup.children[i] as Path).fillAlpha

                    nextFrameColor = (nextFrameGroup.children[i] as Path).fillColor
                    nextFrameAlpha = (nextFrameGroup.children[i] as Path).fillAlpha
                }
                false -> {
                    currentColor = (currentFrameGroup.children[i] as Path).strokeColor
                    currentAlpha = (currentFrameGroup.children[i] as Path).strokeAlpha

                    nextFrameColor = (nextFrameGroup.children[i] as Path).strokeColor
                    nextFrameAlpha = (nextFrameGroup.children[i] as Path).strokeAlpha

                    colorProperty = TimelineProperty.STROKE_COLOR
                    alphaProperty = TimelineProperty.STROKE_ALPHA
                }
            }

            if (currentColor != nextFrameColor) {
                val colorTimelineBlock = TimelineBlock(
                        id = UUID.randomUUID().toString().replace("-",""),
                        layerId = (frameFillOrStrokes.children[i] as Path).id,
                        type = TimelineType.COLOR.id,
                        propertyName = colorProperty.id,

                        fromValue = currentColor!!,
                        toValue = nextFrameColor!!,
                        startTime = currentTime,
                        endTime = currentTime + timeInterval
                )
                blocks.add(colorTimelineBlock)
            }
            if (currentAlpha != nextFrameAlpha) {
                val colorTimelineBlock = TimelineBlock(
                        id = UUID.randomUUID().toString().replace("-",""),
                        layerId = (frameFillOrStrokes.children[i] as Path).id,
                        type = TimelineType.NUMBER.id,
                        propertyName = alphaProperty.id,

                        fromValue = currentAlpha!!,
                        toValue = nextFrameAlpha!!,
                        startTime = currentTime,
                        endTime = currentTime + timeInterval
                )
                blocks.add(colorTimelineBlock)

            }
        } catch (ex: Exception) {
            println(ex.message)
        }
    }


    fun processPaths(id: String, node: Node): Group {
        val paths = (node.value() as NodeList).filter { n ->
            (n as Node).attribute("id").equals(id)
        }

        val children = arrayListOf<PathPrimitive>()

        if (paths.size > 0) {

            val nodeList = ((paths.get(0) as Node).value() as NodeList)

            nodeList.forEachIndexed { index, it ->
                children.add(processPath(UUID.randomUUID().toString().replace("-",""),
                        "$id-$index", (it as Node).attributes() as HashMap<String, String>))
            }
        }

        val group = Group(
                id = UUID.randomUUID().toString().replace("-",""),
                name = id,
                children = children
        )

        return group
    }


    fun composeAnimation(svgHeight: Int, svgWidth: Int, drawingData: HashMap<String, Any>): String {

        val layer = Layer(
                id = UUID.randomUUID().toString().replace("-",""),
                name = "vector",
                width = svgWidth,
                height = svgHeight,
                children = drawingData["frame0"] as ArrayList<Group>
        )
        val jsonObject = mapOf(
                "version" to 1,
                "generatedby" to "GH:/jwill/android-avd-tools",
                "layers" to layersToJSON(layer),
                timelineToJSON(drawingData["animation"] as AnimationTimeline)
        )

        val customJsonOutput = JsonGenerator.Options().excludeNulls().build()

        return customJsonOutput.toJson(jsonObject)
    }

    fun loadSingleFrame(svgHeight: Int, svgWidth: Int, renderLayer_LineSet: Node): String {
        val groups = arrayListOf<Group>()

        groups.add(processPaths("fills", renderLayer_LineSet))
        groups.add(processPaths("strokes", renderLayer_LineSet))

        val layer = Layer(
                id = UUID.randomUUID().toString().replace("-",""),
                name = "vector",
                width = svgWidth,
                height = svgHeight,
                children = groups
        )
        val animation = AnimationTimeline("45")
        val jsonObject = mapOf(
                "version" to 1,
                "layers" to layersToJSON(layer),
                timelineToJSON(animation)
        )


        val customJsonOutput = JsonGenerator.Options().excludeNulls().build()

        return customJsonOutput.toJson(jsonObject)
    }

    fun layersToJSON(layer: Layer): Map<Any, Any> {
        return mapOf(
                "vectorLayer" to layer
        )
    }

    fun timelineToJSON(animation: AnimationTimeline): Pair<Any, Any> {
        return Pair(
                "timeline", mapOf(
                "animation" to animation
        )
        )
    }

    fun checkLineCap(props: HashMap<String, String>): String? {
        val property = props["stroke-linecap"]
        if (property != null) {
            return StrokeLineCap.valueOf(property.toUpperCase()).id;
        }
        return null
    }

    fun checkLineJoin(props: HashMap<String, String>): String? {
        val property = props["stroke-linejoin"]
        if (property != null) {
            return StrokeLineJoin.valueOf(property.toUpperCase()).id
        }
        return null
    }

    fun checkFillType(props: HashMap<String, String>): String? {
        val fillType = props["fill_rule"]
        when (fillType) {
            null -> return null
            // Blender generates some non-compliant SVG
            //"evenOdd", "evenodd" -> return FillType.EVEN_ODD.id
            "nonZero" -> return FillType.NON_ZERO.id
        }
        return null
    }

    fun convertRGB2Hex(color: String): String? {
        return when {
            color.startsWith("#") -> // Color already in hex
                color
            color == "none" -> // No color
                null
            color.startsWith("rgb") -> {
                // color(204, 204, 204)
                val (r, g, b) = color.replace("rgb(", "").replace(")", "").split(",")

                "#%02X%02X%02X".format(r.trim().toInt(), g.trim().toInt(), b.trim().toInt())
            }
            else -> {
                null
            }
        }
    }

    fun processPath(id: String, name: String, props: HashMap<String, String>): PathPrimitive {
        val lineCap = checkLineCap(props)
        val lineJoin = checkLineJoin(props)
        val fillType = checkFillType(props)

        val path = Path(
                id = id,
                name = name,
                pathData = props["d"],

                fillColor = convertRGB2Hex(props["fill"].toString()),
                fillAlpha = props["fill-opacity"]?.toDoubleOrNull(),

                strokeColor = convertRGB2Hex(props["stroke"].toString()),
                strokeWidth = props["stroke-width"]?.toDoubleOrNull(),
                strokeAlpha = props["stroke-opacity"]?.toDoubleOrNull(),

                strokeLineCap = lineCap,
                strokeLineJoin = lineJoin,
                strokeMitterLimit = props["stroke-miterlimit"]?.toIntOrNull(),

                fillType = fillType,

                // not used by SVG it seems
                trimPathEnd = null,
                trimPathStart = null,
                trimPathOffset = null
        )

        return path
    }

}