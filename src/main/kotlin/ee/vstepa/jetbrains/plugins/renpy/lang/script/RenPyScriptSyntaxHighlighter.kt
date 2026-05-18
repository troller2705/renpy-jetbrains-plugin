package ee.vstepa.jetbrains.plugins.renpy.lang.script

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import ee.vstepa.jetbrains.plugins.renpy.lang.script.lexer.RenPyScriptLexerAdapter
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes

class RenPyScriptSyntaxHighlighter : SyntaxHighlighterBase() {

    companion object {
        val KEYWORD: TextAttributesKey = createTextAttributesKey("RENPY_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
        val STRING: TextAttributesKey = createTextAttributesKey("RENPY_STRING", DefaultLanguageHighlighterColors.STRING)
        val NUMBER: TextAttributesKey = createTextAttributesKey("RENPY_NUMBER", DefaultLanguageHighlighterColors.NUMBER)
        val COMMENT: TextAttributesKey = createTextAttributesKey("RENPY_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
        val PYTHON_STATEMENT: TextAttributesKey = createTextAttributesKey("RENPY_PYTHON", DefaultLanguageHighlighterColors.METADATA)

        private val ATTRIBUTES: MutableMap<IElementType, TextAttributesKey> = HashMap()

        init {
            // Strings & Comments
            fillMap(ATTRIBUTES, STRING, RenPyScriptTokenTypes.STRING, RenPyScriptTokenTypes.MULTILINE_DIALOG_STRING)
            fillMap(ATTRIBUTES, COMMENT, RenPyScriptTokenTypes.COMMENT)
            fillMap(ATTRIBUTES, PYTHON_STATEMENT, RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT)
            fillMap(ATTRIBUTES, NUMBER, RenPyScriptTokenTypes.FLOAT_NUMBER, RenPyScriptTokenTypes.PLAIN_NUMBER)

            // Map all structural and logic keywords to the KEYWORD color
            fillMap(
                ATTRIBUTES, KEYWORD,
                RenPyScriptTokenTypes.LABEL_KEYWORD,
                RenPyScriptTokenTypes.DEFINE_KEYWORD,
                RenPyScriptTokenTypes.DEFAULT_KEYWORD,
                RenPyScriptTokenTypes.IMAGE_KEYWORD,
                RenPyScriptTokenTypes.SCREEN_KEYWORD,
                RenPyScriptTokenTypes.TRANSFORM_KEYWORD,
                RenPyScriptTokenTypes.STYLE_KEYWORD,
                RenPyScriptTokenTypes.INIT_KEYWORD,
                RenPyScriptTokenTypes.PYTHON_KEYWORD,
                RenPyScriptTokenTypes.LAYEREDIMAGE_KEYWORD,
                RenPyScriptTokenTypes.IF_KEYWORD,
                RenPyScriptTokenTypes.ELIF_KEYWORD,
                RenPyScriptTokenTypes.ELSE_KEYWORD,
                RenPyScriptTokenTypes.WHILE_KEYWORD,
                RenPyScriptTokenTypes.MENU_KEYWORD,
                RenPyScriptTokenTypes.JUMP_KEYWORD,
                RenPyScriptTokenTypes.CALL_KEYWORD,
                RenPyScriptTokenTypes.RETURN_KEYWORD,
                RenPyScriptTokenTypes.PASS_KEYWORD,
                RenPyScriptTokenTypes.SCENE_KEYWORD,
                RenPyScriptTokenTypes.WITH_KEYWORD,
                RenPyScriptTokenTypes.SHOW_KEYWORD,
                RenPyScriptTokenTypes.HIDE_KEYWORD,
                RenPyScriptTokenTypes.EXPRESSION_KEYWORD,
                RenPyScriptTokenTypes.AS_KEYWORD,
                RenPyScriptTokenTypes.AT_KEYWORD,
                RenPyScriptTokenTypes.BEHIND_KEYWORD,
                RenPyScriptTokenTypes.ONLAYER_KEYWORD,
                RenPyScriptTokenTypes.ZORDER_KEYWORD,
                RenPyScriptTokenTypes.PAUSE_KEYWORD,
                RenPyScriptTokenTypes.PLAY_KEYWORD,
                RenPyScriptTokenTypes.STOP_KEYWORD,
                RenPyScriptTokenTypes.QUEUE_KEYWORD,
                RenPyScriptTokenTypes.VOICE_KEYWORD,
                RenPyScriptTokenTypes.WINDOW_KEYWORD,
                RenPyScriptTokenTypes.MUSIC_KEYWORD,
                RenPyScriptTokenTypes.SOUND_KEYWORD,
                RenPyScriptTokenTypes.SHOW_SCREEN_KEYWORD,
                RenPyScriptTokenTypes.HIDE_SCREEN_KEYWORD,
                RenPyScriptTokenTypes.CALL_SCREEN_KEYWORD,
                RenPyScriptTokenTypes.NONE
            )
        }
    }

    override fun getHighlightingLexer(): Lexer {
        return RenPyScriptLexerAdapter()
    }

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return pack(ATTRIBUTES[tokenType])
    }
}