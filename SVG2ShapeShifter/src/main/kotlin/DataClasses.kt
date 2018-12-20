class Layer(
        var id:String,
        val name:String,
        val type: String = "vector",
        val width:Int,
        val height:Int,
        val alpha:Double = 1.0,
        val children:List<PathPrimitive> = emptyList() // Path or group
) {
    companion object {
        fun parse(map: Map<*, *>) : Layer {
            //parse children
            val children = arrayListOf<PathPrimitive>()
            val rawChildren = map["children"] as ArrayList<Map<*,*>>
            parseChildren(rawChildren, children)

            return Layer(
                    id = map["id"] as String,
                    name = map["name"] as String,
                    width = map["width"] as Int,
                    height = map["height"] as Int,
                    children = children
            )
        }
    }
}

fun parseChildren(rawChildren:ArrayList<Map<*,*>>, childObjectList: ArrayList<PathPrimitive>) {
    for (child:Map<*,*> in rawChildren) {
        when (child["type"].toString()) {
            "group" -> {
                val group = Group.parse(child)
                childObjectList.add(group)
            }
            "path" -> {
                val path = Path.parse(child)
                childObjectList.add(path)
            }
        }

    }
}

class Group(
        var id:String,
        val name:String,
        //val rotation:Int = 0,
        //val scaleX:Double = 1.0,
        //val scaleY:Double = 1.0,
        //val pivotX:Int = 0,
        //val pivotY:Int = 0,
        //val translateX:Int = 0,
        //val translateY:Int = 0,
        val children: List<PathPrimitive>,
        override val type: String = "group"
) : PathPrimitive {
    companion object {
        fun parse(map:Map<*,*>) : Group {
            //parse children
            val children = arrayListOf<PathPrimitive>()
            val rawChildren = map["children"] as ArrayList<Map<*,*>>
            parseChildren(rawChildren, children)

            return Group(
                    id = map["id"].toString(),
                    name = map["name"].toString(),
                    children = children
            )
        }
    }
}


enum class StrokeLineCap(val id:String) {
    BUTT("butt"),
    SQUARE("square"),
    ROUND("round");

    override fun toString() : String = this.id
}

enum class StrokeLineJoin(val id:String) {

    MITER("miter"){
        override fun toString() : String = this.id
    },
    // Blender Freestyle plugin misspelled miter in its exporter
    MITTER("miter") {
        override fun toString() : String = this.id
    },
    ROUND("round"){
        override fun toString() : String = this.id
    },
    BEVEL("bevel");


}

enum class FillType(val id:String) {
    NONE(""){
        override fun toString() : String = ""
    },           // NOT FILLED
    NON_ZERO("nonZero"){
        override fun toString() : String = this.id
    },
    EVEN_ODD("evenOdd"){
        override fun toString() : String = this.id
    }
}

interface PathPrimitive {
    val type:String
}

data class Mask(
        val id:String,
        val name:String,
        val pathData: String,

        override val type: String = "mask"
) : PathPrimitive

data class Path(
        var id:String,
        val name: String,
        val pathData: String?,
        val fillColor: String?,
        val fillAlpha:Double?,

        val strokeColor: String?,
        val strokeWidth: Number? ,
        val strokeAlpha: Number?,

        val strokeLineCap: String?,
        val strokeLineJoin: String?,
        val strokeMitterLimit: Int?,
        val trimPathStart: Number?,
        val trimPathEnd: Number?,
        val trimPathOffset: Number?,
        val fillType: String?,

        override val type: String = "path"

) : PathPrimitive {
    companion object {
        fun parse(map: Map<*, *>) : Path {
            return Path(
                    id = map["id"] as String,
                    name = map["name"] as String,
                    pathData = map["pathData"] as String?,
                    fillColor = map["fillColor"] as String?,
                    fillAlpha = map["fillAlpha"] as Double?,

                    strokeColor = map["strokeColor"] as String?,
                    strokeWidth = map["strokeWidth"] as Number?,
                    strokeAlpha = map["strokeAlpha"] as Number?,

                    strokeLineCap = map["strokeLineCap"] as String?,
                    strokeLineJoin = map["strokeLineJoin"] as String?,
                    strokeMitterLimit = map["strokeMitterLimit"] as Int?,
                    trimPathStart = map["trimPathStart"] as Number?,
                    trimPathEnd = map["trimPathEnd"] as Number?,
                    trimPathOffset = map["trimPathOffset"] as Number?,
                    fillType = map["fillType"] as String?
            )
        }
    }
}



data class AnimationTimeline(
        var id: String,
        val name:String = "anim",
        var duration:Int = 300,
        val blocks:List<TimelineBlock> = emptyList()
) {
    fun fixDuration() {
        println("fixing duration")
        duration = blocks.map { it.endTime }.max()!!
    }

    companion object {
        fun parse(map: Map<*,*>) : AnimationTimeline{
            val blocks = arrayListOf<TimelineBlock>()
            val rawBlocks = map["blocks"] as ArrayList<Map<*,*>>
            for (block in rawBlocks) {
                blocks.add(TimelineBlock.parse(block))
            }

            return AnimationTimeline(
                    id = map["id"].toString(),
                    name = map["name"].toString(),
                    duration = map["duration"] as Int,
                    blocks = blocks
            )
        }
    }
}

data class TimelineBlock(
        var id:String,               // could possibly be rando generated
        val layerId: String,         // id of layer to modify
        val propertyName: String,
        val startTime: Int = 0,
        val endTime: Int = 100,
        val interpolator: Interpolator = Interpolator.LINEAR,
        val type: String,      // need better name, basically data type of thing being modified
        val fromValue:Any,
        val toValue:Any
) {
    companion object {
        fun parse(map: Map<*,*>) : TimelineBlock{
            return TimelineBlock(
                    id = map["id"].toString(),
                    layerId = map["layerId"].toString(),
                    propertyName = map["propertyName"].toString(),
                    startTime = map["startTime"] as Int,
                    endTime = map["endTime"] as Int,
                    interpolator = Interpolator.LINEAR,
                    type = map["type"].toString(),
                    fromValue = map["fromValue"]!!,
                    toValue = map["toValue"]!!)
        }
    }
}

enum class TimelineType(val id:String) {
    PATH("path"),
    NUMBER("number"),
    COLOR("color")
}

enum class TimelineProperty(val id:String) {
    // Group timeline properties
    ROTATION("rotation"),
    SCALE_X("scaleX"),
    SCALE_Y("scaleY"),
    PIVOT_X("pivotX"),
    PIVOT_Y("pivotY"),
    TRANSLATE_X("translateX"),
    TRANSLATE_Y("translateY"),

    // Path timeline properties
    PATH_DATA("pathData"),
    FILL_COLOR("fillColor"),
    FILL_ALPHA("fillAlpha"),
    STROKE_COLOR("strokeColor"),
    STROKE_ALPHA("strokeAlpha"),
    STROKE_WIDTH("strokeWidth"),
    TRIM_PATH_START("trimPathStart"),
    TRIM_PATH_END("trimPathEnd"),
    TRIM_PATH_OFFSET("trimPathOffset")


}

enum class Interpolator{
    FAST_OUT_SLOW_IN,
    FAST_OUT_LINEAR_IN,
    LINEAR_OUT_SLOW_IN,
    ACCELERATE_DECELERATE,
    ACCELERATE,
    DECELERATE,
    LINEAR,
    ANTICIPATE,
    OVERSHOOT,
    BOUNCE,
    ANTICIPATE_OVERSHOOT
}