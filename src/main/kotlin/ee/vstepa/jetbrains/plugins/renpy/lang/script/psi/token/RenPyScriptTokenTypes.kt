package ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token

import com.intellij.psi.tree.IElementType

object RenPyScriptTokenTypes {
    @JvmField val NEW_LINE = RenPyScriptTokenType("NEW_LINE")
    @JvmField val INDENT = RenPyScriptTokenType("INDENT")
    @JvmField val COMMENT = RenPyScriptTokenType("COMMENT")
    @JvmField val STRING = RenPyScriptTokenType("STRING")
    @JvmField val MULTILINE_DIALOG_STRING = RenPyScriptTokenType("MULTILINE_DIALOG_STRING")
    @JvmField val ONE_LINE_PYTHON_STATEMENT = RenPyScriptTokenType("ONE_LINE_PYTHON_STATEMENT")

    // Core
    @JvmField val LABEL_KEYWORD = RenPyScriptTokenType("label")
    @JvmField val DEFINE_KEYWORD = RenPyScriptTokenType("define")
    @JvmField val DEFAULT_KEYWORD = RenPyScriptTokenType("default")
    @JvmField val IMAGE_KEYWORD = RenPyScriptTokenType("image")
    @JvmField val SCREEN_KEYWORD = RenPyScriptTokenType("screen")
    @JvmField val TRANSFORM_KEYWORD = RenPyScriptTokenType("transform")
    @JvmField val STYLE_KEYWORD = RenPyScriptTokenType("style")
    @JvmField val INIT_KEYWORD = RenPyScriptTokenType("init")
    @JvmField val PYTHON_KEYWORD = RenPyScriptTokenType("python")
    @JvmField val LAYEREDIMAGE_KEYWORD = RenPyScriptTokenType("layeredimage")

    // Logic
    @JvmField val IF_KEYWORD = RenPyScriptTokenType("if")
    @JvmField val ELIF_KEYWORD = RenPyScriptTokenType("elif")
    @JvmField val ELSE_KEYWORD = RenPyScriptTokenType("else")
    @JvmField val WHILE_KEYWORD = RenPyScriptTokenType("while")
    @JvmField val MENU_KEYWORD = RenPyScriptTokenType("menu")
    @JvmField val JUMP_KEYWORD = RenPyScriptTokenType("jump")
    @JvmField val CALL_KEYWORD = RenPyScriptTokenType("call")
    @JvmField val RETURN_KEYWORD = RenPyScriptTokenType("return")
    @JvmField val PASS_KEYWORD = RenPyScriptTokenType("pass")

    // Visual & Audio
    @JvmField val SCENE_KEYWORD = RenPyScriptTokenType("scene")
    @JvmField val WITH_KEYWORD = RenPyScriptTokenType("with")
    @JvmField val SHOW_KEYWORD = RenPyScriptTokenType("show")
    @JvmField val HIDE_KEYWORD = RenPyScriptTokenType("hide")
    @JvmField val EXPRESSION_KEYWORD = RenPyScriptTokenType("expression")
    @JvmField val AS_KEYWORD = RenPyScriptTokenType("as")
    @JvmField val AT_KEYWORD = RenPyScriptTokenType("at")
    @JvmField val BEHIND_KEYWORD = RenPyScriptTokenType("behind")
    @JvmField val ONLAYER_KEYWORD = RenPyScriptTokenType("onlayer")
    @JvmField val ZORDER_KEYWORD = RenPyScriptTokenType("zorder")
    @JvmField val PAUSE_KEYWORD = RenPyScriptTokenType("pause")
    @JvmField val PLAY_KEYWORD = RenPyScriptTokenType("play")
    @JvmField val STOP_KEYWORD = RenPyScriptTokenType("stop")
    @JvmField val QUEUE_KEYWORD = RenPyScriptTokenType("queue")
    @JvmField val VOICE_KEYWORD = RenPyScriptTokenType("voice")
    @JvmField val WINDOW_KEYWORD = RenPyScriptTokenType("window")
    @JvmField val MUSIC_KEYWORD = RenPyScriptTokenType("music")
    @JvmField val SOUND_KEYWORD = RenPyScriptTokenType("sound")

    // Screen specific
    @JvmField val SHOW_SCREEN_KEYWORD = RenPyScriptTokenType("show screen")
    @JvmField val HIDE_SCREEN_KEYWORD = RenPyScriptTokenType("hide screen")
    @JvmField val CALL_SCREEN_KEYWORD = RenPyScriptTokenType("call screen")

    // Types & Numbers
    @JvmField val NONE = RenPyScriptTokenType("None")
    @JvmField val FLOAT_NUMBER = RenPyScriptTokenType("FLOAT_NUMBER")
    @JvmField val PLAIN_NUMBER = RenPyScriptTokenType("PLAIN_NUMBER")

    // Punctuation
    @JvmField val DOT = RenPyScriptTokenType(".")
    @JvmField val PARENTHESES_OPEN = RenPyScriptTokenType("(")
    @JvmField val PARENTHESES_CLOSE = RenPyScriptTokenType(")")
    @JvmField val SQUARE_BRACKETS_OPEN = RenPyScriptTokenType("[")
    @JvmField val SQUARE_BRACKETS_CLOSE = RenPyScriptTokenType("]")
    @JvmField val COMMA = RenPyScriptTokenType(",")
    @JvmField val COLON = RenPyScriptTokenType(":")

    // Identifiers
    @JvmField val IDENTIFIER = RenPyScriptTokenType("IDENTIFIER")
    @JvmField val IMAGE_LABEL_IDENTIFIER = RenPyScriptTokenType("IMAGE_LABEL_IDENTIFIER")
}