package ee.vstepa.jetbrains.plugins.renpy.lang.script.lexer;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes;

%%

%class RenPyScriptGeneratedLexer
%extends RenPyScriptLexerBase
%function advance
%type IElementType
%unicode

NEW_LINE = (\r\n|\r|\n)
WS = [ ]+
IDENTIFIER = [a-zA-Z_][a-zA-Z0-9_]*
IMAGE_LABEL_IDENTIFIER = [a-zA-Z0-9_][a-zA-Z0-9_-]*
STRING = \"([^\\\"\n\r]|\\.)*\" | \'([^\\\'\n\r]|\\.)*\'
MULTILINE_DIALOG_STRING = \"([^\\\"]|\\.)*\" | \'([^\\\']|\\.)*\'
COMMENT = \#.*
ONE_LINE_PYTHON_STATEMENT = \$ [^\r\n]*
FLOAT_NUMBER = (0?|[1-9][0-9]*)\.[0-9]+

%%

{NEW_LINE} { return RenPyScriptTokenTypes.NEW_LINE; }
{WS} {
    try {
        char charBeforeMatched = yycharat(-1);
        if (charBeforeMatched == '\n' || charBeforeMatched == '\r') return RenPyScriptTokenTypes.INDENT;
    } catch (IndexOutOfBoundsException e) {}
    return TokenType.WHITE_SPACE;
}
{COMMENT}      { return RenPyScriptTokenTypes.COMMENT; }
{STRING}       { return RenPyScriptTokenTypes.STRING; }
{MULTILINE_DIALOG_STRING} { return RenPyScriptTokenTypes.MULTILINE_DIALOG_STRING; }

// --- Core Structural Keywords ---
"label"        { return RenPyScriptTokenTypes.LABEL_KEYWORD; }
"define"       { return RenPyScriptTokenTypes.DEFINE_KEYWORD; }
"default"      { return RenPyScriptTokenTypes.DEFAULT_KEYWORD; }
"image"        { return RenPyScriptTokenTypes.IMAGE_KEYWORD; }
"screen"       { return RenPyScriptTokenTypes.SCREEN_KEYWORD; }
"transform"    { return RenPyScriptTokenTypes.TRANSFORM_KEYWORD; }
"style"        { return RenPyScriptTokenTypes.STYLE_KEYWORD; }
"init"         { return RenPyScriptTokenTypes.INIT_KEYWORD; }
"python"       { return RenPyScriptTokenTypes.PYTHON_KEYWORD; }
"layeredimage" { return RenPyScriptTokenTypes.LAYEREDIMAGE_KEYWORD; }

// --- Logic & Control Flow ---
"if"           { return RenPyScriptTokenTypes.IF_KEYWORD; }
"elif"         { return RenPyScriptTokenTypes.ELIF_KEYWORD; }
"else"         { return RenPyScriptTokenTypes.ELSE_KEYWORD; }
"while"        { return RenPyScriptTokenTypes.WHILE_KEYWORD; }
"menu"         { return RenPyScriptTokenTypes.MENU_KEYWORD; }
"jump"         { return RenPyScriptTokenTypes.JUMP_KEYWORD; }
"call"         { return RenPyScriptTokenTypes.CALL_KEYWORD; }
"return"       { return RenPyScriptTokenTypes.RETURN_KEYWORD; }
"pass"         { return RenPyScriptTokenTypes.PASS_KEYWORD; }

// --- Visual & Audio ---
"scene"        { return RenPyScriptTokenTypes.SCENE_KEYWORD; }
"show"         { return RenPyScriptTokenTypes.SHOW_KEYWORD; }
"hide"         { return RenPyScriptTokenTypes.HIDE_KEYWORD; }
"with"         { return RenPyScriptTokenTypes.WITH_KEYWORD; }
"as"           { return RenPyScriptTokenTypes.AS_KEYWORD; }
"at"           { return RenPyScriptTokenTypes.AT_KEYWORD; }
"behind"       { return RenPyScriptTokenTypes.BEHIND_KEYWORD; }
"onlayer"      { return RenPyScriptTokenTypes.ONLAYER_KEYWORD; }
"zorder"       { return RenPyScriptTokenTypes.ZORDER_KEYWORD; }
"expression"   { return RenPyScriptTokenTypes.EXPRESSION_KEYWORD; }
"pause"        { return RenPyScriptTokenTypes.PAUSE_KEYWORD; }
"play"         { return RenPyScriptTokenTypes.PLAY_KEYWORD; }
"stop"         { return RenPyScriptTokenTypes.STOP_KEYWORD; }
"queue"        { return RenPyScriptTokenTypes.QUEUE_KEYWORD; }
"voice"        { return RenPyScriptTokenTypes.VOICE_KEYWORD; }
"window"       { return RenPyScriptTokenTypes.WINDOW_KEYWORD; }
"music"        { return RenPyScriptTokenTypes.MUSIC_KEYWORD; }
"sound"        { return RenPyScriptTokenTypes.SOUND_KEYWORD; }

// --- Screen Specific ---
"show screen"  { return RenPyScriptTokenTypes.SHOW_SCREEN_KEYWORD; }
"hide screen"  { return RenPyScriptTokenTypes.HIDE_SCREEN_KEYWORD; }
"call screen"  { return RenPyScriptTokenTypes.CALL_SCREEN_KEYWORD; }

// --- Python & Types ---
{ONE_LINE_PYTHON_STATEMENT} { return RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT; }
"None"         { return RenPyScriptTokenTypes.NONE; }
{FLOAT_NUMBER} { return RenPyScriptTokenTypes.FLOAT_NUMBER; }

// --- Punctuation ---
"."            { return RenPyScriptTokenTypes.DOT; }
"("            { return RenPyScriptTokenTypes.PARENTHESES_OPEN; }
")"            { return RenPyScriptTokenTypes.PARENTHESES_CLOSE; }
"["            { return RenPyScriptTokenTypes.SQUARE_BRACKETS_OPEN; }
"]"            { return RenPyScriptTokenTypes.SQUARE_BRACKETS_CLOSE; }
","            { return RenPyScriptTokenTypes.COMMA; }
":"            { return RenPyScriptTokenTypes.COLON; }

// --- Identifiers ---
{IDENTIFIER}   { return RenPyScriptTokenTypes.IDENTIFIER; }
{IMAGE_LABEL_IDENTIFIER} {
    CharSequence cs = yytext();
    boolean allDigits = true;
    for (int i = 0; i < cs.length(); i++) {
        if (!Character.isDigit(cs.charAt(i))) {
            allDigits = false;
            break;
        }
    }
    if (allDigits) {
        return RenPyScriptTokenTypes.PLAIN_NUMBER;
    }
    return RenPyScriptTokenTypes.IMAGE_LABEL_IDENTIFIER;
}

.              { return TokenType.BAD_CHARACTER; }