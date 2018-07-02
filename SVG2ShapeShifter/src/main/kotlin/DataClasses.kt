class Layer(
        val id:String,
        val name:String,
        val type: String = "vector",
        val width:Int,
        val height:Int,
        val alpha:Double = 1.0,
        val children:List<Any> = emptyList() // Path or group
)

class Group(
        val id:String,
        val name:String,
        val rotation:Int = 0,
        val scaleX:Double = 1.0,
        val scaleY:Double = 1.0,
        val pivotX:Int = 0,
        val pivotY:Int = 0,
        val translateX:Int = 0,
        val translateY:Int = 0,
        val children: List<PathPrimitive>,
        val type: String = "group"
)


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

open class PathPrimitive {
    open val type:String = ""
}

data class Mask(
        val id:String,
        val name:String,
        val pathData: String,

        override val type: String = "mask"
) : PathPrimitive()

data class Path(
        val id:String,
        val name: String,
        val pathData: String?,
        val fillColor: String?,
        val fillAlpha:Double?,

        val strokeColor: String?,
        val strokeWidth: Double?,
        val strokeAlpha: Double?,

        val strokeLineCap: String?,
        val strokeLineJoin: String?,
        val strokeMitterLimit: Int?,
        val trimPathStart: Double?,
        val trimPathEnd: Double?,
        val trimPathOffset: Double?,
        val fillType: String?,

        override val type: String = "path"

) : PathPrimitive()



data class AnimationTimeline(
        val id: String,
        val name:String = "anim",
        val duration:Int = 300,
        val blocks:List<TimelineBlock> = emptyList()
)

data class TimelineBlock(
        val id:String,               // could possibly be rando generated
        val layerId: String,         // id of layer to modify
        val propertyName: String,
        val startTime: Int = 0,
        val endTime: Int = 100,
        val interpolator: Interpolator = Interpolator.LINEAR,
        val type: String,      // need better name, basically data type of thing being modified
        val fromValue:Any,
        val toValue:Any
)

enum class TimelineType(val id:String) {
    PATH("path"),
    NUMBER("number")
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