import groovy.json.JsonGenerator
import groovy.json.JsonOutput
import groovy.util.Node
import groovy.util.NodeList
import groovy.util.XmlParser
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


fun main(args: Array<String>) {
    // Load SVG generated from Blender
    //val file = File("examples/0001.svg")
    val file = File("examples/suzanne.svg")

    val svg  = XmlParser().parse(file)

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
            val outputFile = File(UUID.randomUUID().toString()+".shapeshifter")
            outputFile.writeText(JsonOutput.prettyPrint(json))
        }

        false -> {
            val json = loadSingleFrame(svgHeight, svgWidth, renderLayer_LineSet)
            val outputFile = File(UUID.randomUUID().toString()+".shapeshifter")
            outputFile.writeText(JsonOutput.prettyPrint(json))
        }
    }
}

fun isMultiFrameSVG(node:Node) : Boolean {
    val frames = (node.value() as NodeList)
    val item = frames.first() as Node
    return item.attribute("id") == "frame_0001"
}

fun loadFrameZeroAndAnimation(renderLayer_LineSet: Node): HashMap<String,Any> {
    val frames = (renderLayer_LineSet.value() as NodeList)

    val frame0 = frames.first() as Node
    val frame0Fills = processPaths("fills", frame0)
    val frame0Strokes = processPaths("strokes", frame0)

    val lastFrame = frames.last() as Node
    val lastFrameFills = processPaths("fills", lastFrame)
    val lastFrameStrokes = processPaths("strokes", lastFrame)

    // TODO() -- iterate through the paths
    // Should I throw an error if the source and destination paths are unequal

    val blocks = arrayListOf<TimelineBlock>()

    frame0Fills.children.forEachIndexed{ index, path ->
        val timelineBlock = TimelineBlock(
                id = UUID.randomUUID().toString(),
                layerId = (frame0Fills.children[index] as Path).id,
                type = TimelineType.PATH.id,
                fromValue = (frame0Fills.children[index] as Path).pathData!!,
                toValue = (lastFrameFills.children[index] as Path).pathData!!,
                propertyName = TimelineProperty.PATH_DATA.id
        )
        blocks.add(timelineBlock)
    }

    frame0Strokes.children.forEachIndexed { index, path ->
        val timelineBlock = TimelineBlock(
                id = UUID.randomUUID().toString(),
                layerId = (frame0Strokes.children[index] as Path).id,
                type = TimelineType.PATH.id,
                fromValue = (frame0Strokes.children[index] as Path).pathData!!,
                toValue = (lastFrameStrokes.children[index] as Path).pathData!!,
                propertyName = TimelineProperty.PATH_DATA.id
        )
        blocks.add(timelineBlock)
    }

    val animation = AnimationTimeline(UUID.randomUUID().toString(),
            blocks = blocks
    )


    return hashMapOf("frame0" to arrayListOf(frame0Fills, frame0Strokes),
            "animation" to animation)
}


fun processPaths(id:String, node:Node) : Group {
    val paths = (node.value() as NodeList).filter {
        n -> (n as Node).attribute("id").equals(id)
    }

    val children = arrayListOf<PathPrimitive>()

    if(paths.size > 0) {

        val nodeList = ((paths.get(0) as Node).value() as NodeList)

        nodeList.forEachIndexed { index, it ->
            children.add(processPath(UUID.randomUUID().toString(), "$id-$index", (it as Node).attributes() as HashMap<String, String>))
        }
    }

    val group = Group(
            id = UUID.randomUUID().toString(),
            name = id,
            children = children
    )

    return group
}


fun composeAnimation(svgHeight: Int, svgWidth: Int, drawingData: HashMap<String,Any>) : String {

    val layer = Layer(
            id = UUID.randomUUID().toString(),
            name = "vector",
            width = svgWidth,
            height = svgHeight,
            children = drawingData["frame0"] as ArrayList<Group>
    )
    val jsonObject = mapOf(
            "version" to 1,
            "layers" to layersToJSON(layer),
            timelineToJSON(drawingData["animation"] as AnimationTimeline)
    )

    val customJsonOutput = JsonGenerator.Options().excludeNulls().build()

    return customJsonOutput.toJson(jsonObject)
}

fun loadSingleFrame(svgHeight:Int, svgWidth:Int, renderLayer_LineSet:Node) : String {
    val groups = arrayListOf<Group>()

    groups.add(processPaths("fills", renderLayer_LineSet))
    groups.add(processPaths("strokes", renderLayer_LineSet))

    val layer = Layer(
            id = UUID.randomUUID().toString(),
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

fun layersToJSON(layer:Layer) : Map<Any, Any> {
    return mapOf(
            "vectorLayer" to layer
    )
}

fun timelineToJSON(animation: AnimationTimeline) : Pair<Any, Any>{
    return Pair(
            "timeline", mapOf(
                    "animation" to animation
            )
    )
}

fun checkLineCap(props: HashMap<String, String>) : String?{
    val property = props["stroke-linecap"]
    if (property != null) {
        return StrokeLineCap.valueOf(property.toUpperCase()).id;
    }
    return null
}

fun checkLineJoin(props: HashMap<String, String>) : String?{
    val property = props["stroke-linejoin"]
    if (property != null) {
        return StrokeLineJoin.valueOf(property.toUpperCase()).id;
    }
    return null
}

fun checkFillType( props:HashMap<String,String>) : String? {
    val fillType = props["fill_rule"]
    when(fillType) {
        null -> return null
        "evenOdd", "evenodd" -> return FillType.EVEN_ODD.id
        "nonZero" -> return FillType.NON_ZERO.id
    }
    return null
}

fun convertRGB2Hex(color:String) : String? {
    return when {
        color.startsWith("#") -> // Color already in hex
            color
        color == "none" -> // No color
            null
        color.startsWith("rgb") -> {
            // color(204, 204, 204)
            val (r,g,b) = color.replace("rgb(", "").replace(")","").split(",")

            "#%02X%02X%02X".format(r.trim().toInt(), g.trim().toInt(), b.trim().toInt())
        }
        else -> {
            null
        }
    }
}

fun processPath(id:String, name: String, props:HashMap<String,String>) : PathPrimitive  {
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

    println(JsonOutput.toJson(path))
    return path
}

