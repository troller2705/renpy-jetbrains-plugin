package ee.vstepa.jetbrains.plugins.renpy.lang.script.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilder.Marker
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.NlsContexts.ParsingError
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.RenPyScriptElementType
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.RenPyScriptElementTypes
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.audio.RenPyScriptAudioControlStmtElementType
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.element.type.conditional.RenPyScriptConditionalSubStmtElementType
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenSets
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenType
import ee.vstepa.jetbrains.plugins.renpy.lang.script.psi.token.RenPyScriptTokenTypes

val LOG = Logger.getInstance(RenPyScriptParsing::class.java)

open class RenPyScriptParsing(private val builder: PsiBuilder) {

    open fun parseScript() {
        mark().also {
            // Skipping new lines at the beginning of a file
            while (token() == RenPyScriptTokenTypes.NEW_LINE) advance()

            if (token() == RenPyScriptTokenTypes.INDENT) {
                while (!eof()) advance()
                it.error("Unexpected indentation at the start of file")
            } else {
                parseGeneralStatementsList(getCurrentLineIndentLevel() - 1)
                while (!eof()) {
                    error("Unexpected end of file")
                    advance()
                }
                it.done(RenPyScriptElementTypes.REN_PY_SCRIPT)
            }
        }
    }

    protected open fun parseStatementsList(
        exitIfIndentEqualsOrLess: Int,
        stmtStartTokenChecks: () -> Boolean,
        functionToCallInCaseOfUnexpectedIndent: (Int) -> Boolean
    ): Boolean {
        val statementsListMarker = mark()

        while (!eof()) {
            token().also {
                if (it != null && !isTokenNewLine(it) && it != RenPyScriptTokenTypes.INDENT) break
                advance()
            }
        }

        val initialIndentLevel = getCurrentLineIndentLevel()
        while (!eof()) {
            // Skipping trailing new lines, indents on empty lines, etc
            val token = token()
            if (isTokenNewLine(token)) {
                advance()
                continue
            } else if (token == RenPyScriptTokenTypes.INDENT) {
                val nextToken = tokenAhead(1)
                if (nextToken == null || isTokenNewLine(nextToken)) {
                    advance()
                    continue
                }
            }

            val currentLineIndentLevel = getCurrentLineIndentLevel()
            if (currentLineIndentLevel <= exitIfIndentEqualsOrLess) break
            if (currentLineIndentLevel != initialIndentLevel) {
                error("Indentation mismatch")
            }
            val success: Boolean = when (token) {
                // Indents are handled by statements, if we encounter indent that is bigger then our own as statement start char during parsing
                // statements list - indentation error
                RenPyScriptTokenTypes.INDENT if currentLineIndentLevel > initialIndentLevel -> {
                    markAdvanceError("Line is indented, but the preceding statement does not expect a block. Please check this line's indentation.")
                    functionToCallInCaseOfUnexpectedIndent(initialIndentLevel)
                }

                // Skipping indents that are of the same level
                RenPyScriptTokenTypes.INDENT -> {
                    advance()
                    true
                }

                else -> stmtStartTokenChecks()
            }

            if (!success) advanceToNewLineOrEof()
        }

        statementsListMarker.done(RenPyScriptElementTypes.STMTS_LIST)
        return true
    }

    protected open fun parseGeneralStatementsList(exitIfIndentEqualsOrLess: Int): Boolean = parseStatementsList(
        exitIfIndentEqualsOrLess,
        {
            val token = token()
            when {
                // Start of dialog statement, with character or without
                isTokenDialogueStatementStart(token) -> parseDialogStatement()

                // Start of label, menu, if, while and other block statements
                isTokenBlockStatementStart(token) -> parseBlockStatement()

                // Start of jump ... or jump expression ...
                token == RenPyScriptTokenTypes.JUMP_KEYWORD -> parseJumpStatement()

                // Start of 'call ...' or 'call expression ...' or 'call ... from ...', etc
                token == RenPyScriptTokenTypes.CALL_KEYWORD -> parseCallStatement()

                // Start of show, scene, hide or with statement
                isTokenImageDisplayControlStatementStart(token) -> parseImageDisplayControlStatement()

                // One-line python statement, e.g., $ var_1 += 2
                token == RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT -> parseQuickPythonStatement()

                // 'pass' statement in statements list
                token == RenPyScriptTokenTypes.PASS_KEYWORD -> parsePassStatement()

                // 'return' statement in statements list
                token == RenPyScriptTokenTypes.RETURN_KEYWORD -> parseReturnStatement()

                // 'play', 'stop' or 'queue' statements
                isTokenAudioControlStatementStart(token) -> parseAudioControlStatement()

                // 'pause' statement
                token == RenPyScriptTokenTypes.PAUSE_KEYWORD -> parsePauseStatement()

                // ---------------------------------------------------------
                // NEW: Generic fallback parser for structural/UI keywords
                // ---------------------------------------------------------
                token == RenPyScriptTokenTypes.DEFINE_KEYWORD ||
                        token == RenPyScriptTokenTypes.DEFAULT_KEYWORD ||
                        token == RenPyScriptTokenTypes.IMAGE_KEYWORD ||
                        token == RenPyScriptTokenTypes.SCREEN_KEYWORD ||
                        token == RenPyScriptTokenTypes.TRANSFORM_KEYWORD ||
                        token == RenPyScriptTokenTypes.STYLE_KEYWORD ||
                        token == RenPyScriptTokenTypes.INIT_KEYWORD ||
                        token == RenPyScriptTokenTypes.PYTHON_KEYWORD ||
                        token == RenPyScriptTokenTypes.LAYEREDIMAGE_KEYWORD ||
                        token == RenPyScriptTokenTypes.VOICE_KEYWORD ||
                        token == RenPyScriptTokenTypes.WINDOW_KEYWORD ||
                        token == RenPyScriptTokenTypes.MUSIC_KEYWORD ||
                        token == RenPyScriptTokenTypes.SOUND_KEYWORD -> parseGenericDeclarationStatement()

                else -> {
                    markAdvanceError("Unexpected statement start token: $token")
                    false
                }
            }
        },
        this::parseGeneralStatementsList
    )

    protected open fun parseGenericDeclarationStatement(): Boolean {
        val currentIndentLevel = getCurrentLineIndentLevel()
        val stmtMarker = mark()

        // Consume the start token (e.g., define, init, screen)
        markAndDoneGeneralKeyword()

        var hasColon = false
        // Read the rest of the line until a colon or newline is hit
        while (!eof() && !isTokenNewLine()) {
            if (token() == RenPyScriptTokenTypes.COLON) {
                hasColon = true
                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_COLON)
                break
            }
            advance()
        }

        var success = verifyTokenIsNewLineOrEof("New line is expected at the end of the statement")

        // If a colon was found, this statement expects an indented block underneath it
        if (hasColon && success && !eof()) {

            // FIX: Do NOT call parseGeneralStatementsList(currentIndentLevel)
            // Python, Screen, and Transform blocks use their own internal syntax.
            // Instead, we just sweep over the entire indented block safely
            // without trying to validate it as Ren'Py dialogue.
            mark().also {
                advanceToDedentOfCurrentBlockOrEof(currentIndentLevel)
                it.done(RenPyScriptElementTypes.STMTS_LIST)
            }
        }

        // Drop the marker so we don't break the existing PSI tree structure
        stmtMarker.drop()
        return success
    }

    protected open fun parseBlockStatement(): Boolean {
        return when (val token = token()) {
            RenPyScriptTokenTypes.LABEL_KEYWORD -> parseLabel()
            RenPyScriptTokenTypes.MENU_KEYWORD -> parseMenu()
            RenPyScriptTokenTypes.IF_KEYWORD -> parseConditionalStmt()
            RenPyScriptTokenTypes.WHILE_KEYWORD -> parseConditionalSubStmt(RenPyScriptElementTypes.WHILE_STMT)
            else -> {
                error("Invalid block statement start token: $token")
                false
            }
        }
    }

    protected open fun parseLabel(): Boolean {
        if (token() != RenPyScriptTokenTypes.LABEL_KEYWORD) {
            error("Invalid label start token: ${token()}")
            return false
        }

        val currentIndentLevel = getCurrentLineIndentLevel()

        val labelMarker = mark()
        val labelStatementMarker = mark()

        markAndDoneGeneralKeyword()

        var success = true
        val labelNameMarker = mark()
        if (token() == RenPyScriptTokenTypes.IDENTIFIER) {
            advance()
            labelNameMarker.done(RenPyScriptElementTypes.LABEL_STMT_NAME)
        } else {
            error("Label name expected")
            labelNameMarker.drop()
            success = false
        }

        val labelColonMarker = mark()
        if (token() == RenPyScriptTokenTypes.COLON) {
            advance()
            labelColonMarker.done(RenPyScriptElementTypes.GEN_STMT_COLON)
        } else {
            error("Colon is expected at the end of the label statement")
            labelColonMarker.drop()
            success = false
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the label statement") && success

        labelStatementMarker.done(RenPyScriptElementTypes.LABEL_STMT)

        if (success && !eof()) success = parseGeneralStatementsList(currentIndentLevel)

        labelMarker.done(RenPyScriptElementTypes.LABEL)
        return success
    }

    protected open fun parseMenu(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.MENU_KEYWORD) {
                error("Invalid 'menu' start token: $it")
                return false
            }
        }

        val currentIndentLevel = getCurrentLineIndentLevel()

        val stmtMarker = mark()

        markAndDoneGeneralKeyword()

        // Menu can have an optional name. Like label, but optional
        if (token() == RenPyScriptTokenTypes.IDENTIFIER) markAdvanceDone(RenPyScriptElementTypes.MENU_STMT_NAME)

        var success =
            if (token() == RenPyScriptTokenTypes.PARENTHESES_OPEN) parseGenStmtArgsList()
            else true

        if (token() == RenPyScriptTokenTypes.COLON) markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_COLON)
        else {
            error("Colon is expected at the end of the 'menu' statement")
            success = false
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'menu' statement") && success

        if (success && !eof()) success = parseMenuStatementsList(currentIndentLevel)

        stmtMarker.done(RenPyScriptElementTypes.MENU_STMT)
        return success
    }

    protected open fun parseMenuStatementsList(exitIfIndentEqualsOrLess: Int): Boolean = parseStatementsList(
        exitIfIndentEqualsOrLess,
        {
            val token = token()
            when {
                // 'set' statement of menu
                token == RenPyScriptTokenTypes.IDENTIFIER && tokenText().equals("set") -> parseMenuSetStatement()

                // Start of dialog statement inside menu. We check not only start token, but whole structure,
                // because dialog line start token is the same as menu choice start token
                isCurrentlyAtStartOfDialogStatementStructure() -> parseDialogStatement()

                // Start of menu choice. E.g., "Go right":
                isTokenMenuStatementChoiceCaption(token) -> parseMenuStatementChoice()

                else -> {
                    markAdvanceError("Unexpected menu indented block statement start token: $token")
                    false
                }
            }
        },
        this::parseMenuStatementsList
    )

    protected open fun parseMenuSetStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.IDENTIFIER) {
                error("Invalid 'set' menu statement start token: $it")
                return false
            }
        }
        tokenText().also {
            if (!it.equals("set")) {
                error("Invalid 'set' menu statement start token: $it")
                return false
            }
        }

        val stmtMarker = mark()

        markAndDoneGeneralKeyword()

        var success = true

        if (token() == RenPyScriptTokenTypes.IDENTIFIER) markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
        else {
            error("'set' menu statement set or list identifier expected")
            success = false
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'set' menu statement") && success

        stmtMarker.done(RenPyScriptElementTypes.MENU_STMT_SET_STMT)
        return success
    }

    protected open fun parseMenuStatementChoice(): Boolean {
        token().also {
            if (!isTokenMenuStatementChoiceCaption(it)) {
                error("Invalid 'menu' statement choice start token: $it")
                return false
            }
        }

        val currentIndentLevel = getCurrentLineIndentLevel()

        val stmtMarker = mark()

        markAdvanceDone(RenPyScriptElementTypes.MENU_STMT_CHOICE_CAPTION)

        var success =
            if (token() == RenPyScriptTokenTypes.PARENTHESES_OPEN) parseGenStmtArgsList()
            else true

        if (token() == RenPyScriptElementTypes.MENU_STMT_CHOICE_IF_CLAUSE.keywordTokenType)
            success = parseConditionalSubStmt(RenPyScriptElementTypes.MENU_STMT_CHOICE_IF_CLAUSE)

        if (token() == RenPyScriptTokenTypes.COLON) markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_COLON)
        else {
            error("Colon is expected at the end of the 'menu' statement choice")
            success = false
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'menu' statement choice") && success

        if (success && !eof()) success = parseGeneralStatementsList(currentIndentLevel)

        stmtMarker.done(RenPyScriptElementTypes.MENU_STMT_CHOICE)
        return success
    }

    protected open fun parseConditionalStmt(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.IF_KEYWORD) {
                error("Invalid conditional statement start token: $it")
                return false
            }
        }

        val conditionalStmtMarker = mark()

        try {
            if (!parseConditionalSubStmt(RenPyScriptElementTypes.IF_STMT)) {
                error("Failed to parse conditional statement initial '${RenPyScriptElementTypes.IF_STMT.stmtName}' sub statement")
                return false
            }

            while (token() == RenPyScriptElementTypes.ELIF_STMT.keywordTokenType) {
                if (!parseConditionalSubStmt(RenPyScriptElementTypes.ELIF_STMT)) {
                    error("Failed to parse conditional statement '${RenPyScriptElementTypes.ELIF_STMT.stmtName}' sub statement")
                    return false
                }
            }

            if (token() == RenPyScriptElementTypes.ELSE_STMT.keywordTokenType && !parseConditionalSubStmt(RenPyScriptElementTypes.ELSE_STMT)) {
                error("Failed to parse conditional statement final '${RenPyScriptElementTypes.ELSE_STMT.stmtName}' sub statement")
                return false
            }
        } finally {
            conditionalStmtMarker.done(RenPyScriptElementTypes.CONDITIONAL_STMT)
        }

        return true
    }

    protected open fun parseConditionalSubStmt(stmtElementType: RenPyScriptConditionalSubStmtElementType): Boolean {
        token().also {
            if (it != stmtElementType.keywordTokenType) {
                error("Invalid '${stmtElementType.stmtName}' statement start token: $it")
                return false
            }
        }

        val currentIndentLevel = getCurrentLineIndentLevel()

        val stmtMarker = mark()

        markAndDoneGeneralKeyword()

        var success = true
        if (stmtElementType.hasCondition) {
            mark().also {
                var foundAnyCondition = false
                var parenthesesLevel = 0
                while (!eof()) {
                    val token = token()
                    if (token == RenPyScriptTokenTypes.COLON) break

                    foundAnyCondition = true

                    when {
                        token == RenPyScriptTokenTypes.PARENTHESES_OPEN -> parenthesesLevel++
                        token == RenPyScriptTokenTypes.PARENTHESES_CLOSE -> {
                            if (parenthesesLevel > 0) parenthesesLevel--
                            else {
                                error("Unexpected closing parenthesis")
                                success = false
                            }
                        }
                        isTokenNewLine(token) || isTokenIndent(token) -> {
                            if (parenthesesLevel == 0) {
                                error("New lines are only allowed in '${stmtElementType.stmtName}' statement's condition inside parentheses, not here")
                                success = false
                                break
                            }
                        }
                    }

                    advance()
                }
                if (!foundAnyCondition) {
                    error("No condition found in '${stmtElementType.stmtName}' statement")
                    success = false
                }
                if (parenthesesLevel > 0) {
                    error("$parenthesesLevel parentheses are not closed inside '${stmtElementType.stmtName}' statement condition")
                    success = false
                }
                it.done(RenPyScriptElementTypes.CONDITIONAL_STMT_CONDITION)
            }
        }

        if (!stmtElementType.hasIndentedBlock) {
            // For the if statements like at the end of menu statement,
            // when we don't need to make colon and following statements list a part of IF statement
            stmtMarker.done(stmtElementType)
            return success
        }

        if (token() == RenPyScriptTokenTypes.COLON) markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_COLON)
        else {
            error("Colon is expected at the end of the '${stmtElementType.stmtName}' statement")
            success = false
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the '${stmtElementType.stmtName}' statement") && success

        if (success && !eof()) {
            success = parseGeneralStatementsList(currentIndentLevel)
            while (!eof() && (isTokenNewLine() || isTokenIndent())) advance()
        }

        stmtMarker.done(stmtElementType)
        return success
    }

    protected open fun parseDialogStatement(): Boolean {
        if (!isTokenDialogueStatementStart()) {
            error("Invalid 'dialog' statement start token: ${token()}")
            return false
        }

        val dialogStatementMarker = mark()

        when (token()) {
            RenPyScriptTokenTypes.IDENTIFIER -> {
                markAdvanceDone(RenPyScriptElementTypes.DIALOG_STMT_IDENTIFIER)
            }
            RenPyScriptTokenTypes.STRING if (isTokenDialogueStatementTextStart(tokenAhead())) -> {
                // Our token text identifier is also a text line
                // Dialog line looks like this: "John" "Hello, my name is John!"
                markAdvanceDone(RenPyScriptElementTypes.DIALOG_STMT_TEXT_IDENTIFIER)
            }
        }

        var success = true
        mark().also {
            if (isTokenDialogueStatementTextStart()) {
                advance()
                it.done(RenPyScriptElementTypes.DIALOG_STMT_TEXT)
            } else {
                error("Dialog statement text expected")
                it.drop()
                success = false
            }
        }

        if (token() == RenPyScriptTokenTypes.PARENTHESES_OPEN) success = parseGenStmtArgsList() && success
        if (token() == RenPyScriptTokenTypes.WITH_KEYWORD) success = parseGenStmtWithClause("dialog") && success

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'dialog' statement") && success

        dialogStatementMarker.done(RenPyScriptElementTypes.DIALOG_STMT)
        return success
    }

    protected open fun parseJumpStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.JUMP_KEYWORD) {
                error("Invalid 'jump' statement start token: $it")
                return false
            }
        }

        val jumpStatementMarker = mark()

        markAndDoneGeneralKeyword()

        var success = true
        when (token()) {
            RenPyScriptTokenTypes.EXPRESSION_KEYWORD -> {
                val jumpStatementExpressionMarker = mark()
                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                if (isTokenExpressionValue()) {
                    markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
                } else {
                    error("Jump statement expression expected")
                    success = false
                }

                jumpStatementExpressionMarker.done(RenPyScriptElementTypes.GEN_STMT_EXPRESSION)
            }
            RenPyScriptTokenTypes.IDENTIFIER -> markAdvanceDone(RenPyScriptElementTypes.JUMP_STMT_TARGET)
            else -> {
                error("Jump statement target or expression expected")
                success = false
            }
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the jump statement") && success

        jumpStatementMarker.done(RenPyScriptElementTypes.JUMP_STMT)
        return success
    }

    protected open fun parseCallStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.CALL_KEYWORD) {
                error("Invalid 'call' statement start token: $it")
                return false
            }
        }

        val stmtMarker = mark()

        markAndDoneGeneralKeyword()

        var success = true
        var expression = false
        when (token()) {
            RenPyScriptTokenTypes.EXPRESSION_KEYWORD -> {
                expression = true
                val stmtExpressionMarker = mark()
                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                if (isTokenExpressionValue()) {
                    markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
                } else {
                    error("'call' statement expression expected")
                    success = false
                }

                stmtExpressionMarker.done(RenPyScriptElementTypes.GEN_STMT_EXPRESSION)
            }
            RenPyScriptTokenTypes.IDENTIFIER -> markAdvanceDone(RenPyScriptElementTypes.CALL_STMT_TARGET)
            else -> {
                error("'call' statement target or expression expected")
                success = false
            }
        }

        var strictArguments = false
        if (expression) {
            when (token()) {
                RenPyScriptTokenTypes.PARENTHESES_OPEN -> {
                    error("To pass arguments to the 'call' statement when an 'expression' is used, need to use keyword 'pass' before arguments list")
                    success = false
                }
                RenPyScriptTokenTypes.PASS_KEYWORD -> {
                    strictArguments = true
                    markAndDoneGeneralKeyword()
                }
            }
        } else {
            if (token() == RenPyScriptTokenTypes.PASS_KEYWORD) {
                markAdvanceError("'pass' keyword is expected in 'call' statement only when passing arguments to an 'expression'")
                success = false
            }
        }

        if (token() == RenPyScriptTokenTypes.PARENTHESES_OPEN) success = parseGenStmtArgsList() && success
        else if (strictArguments) {
            error("'call' statement expects arguments list here strictly")
            success = false
        }

        if (token() == RenPyScriptTokenTypes.IDENTIFIER && tokenText().equals("from")) {
            mark().also {
                markAndDoneGeneralKeyword()

                if (token() == RenPyScriptTokenTypes.IDENTIFIER) markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
                else {
                    error("'call' statement 'from' clause value expected")
                    success = false
                }

                it.done(RenPyScriptElementTypes.CALL_STMT_FROM_CLAUSE)
            }
        }

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'call' statement") && success

        stmtMarker.done(RenPyScriptElementTypes.JUMP_STMT)
        return success
    }

    protected open fun parseImageDisplayControlStatement(): Boolean {
        return when (val token = token()) {
            RenPyScriptTokenTypes.SHOW_KEYWORD -> parseShowStatement()
            RenPyScriptTokenTypes.SCENE_KEYWORD -> parseSceneStatement()
            RenPyScriptTokenTypes.HIDE_KEYWORD -> parseHideStatement()
            RenPyScriptTokenTypes.WITH_KEYWORD -> parseWithStatement()

            else -> {
                error("Invalid image display control statement start token: $token")
                return false
            }
        }
    }

    protected open fun parseShowOrSceneStatement(
        stmtKeywordToken: RenPyScriptTokenType,
        allowSingleKeyword: Boolean,
        stmtElementType: RenPyScriptElementType,
    ): Boolean {
        val stmtName = when (stmtKeywordToken) {
            RenPyScriptTokenTypes.SHOW_KEYWORD -> "show"
            RenPyScriptTokenTypes.SCENE_KEYWORD -> "scene"
            else -> {
                error("'show' or 'scene' statement expected, got: $stmtKeywordToken")
                return false
            }
        }

        token().also {
            if (it != stmtKeywordToken) {
                error("Invalid '$stmtName' statement start token: $it")
                return false
            }
        }

        val currentIndentLevel = getCurrentLineIndentLevel()

        val stmtMarker = mark()

        // Mark keyword token with element type
        markAndDoneGeneralKeyword()

        var success = true
        val token = token()
        when {
            token == RenPyScriptTokenTypes.EXPRESSION_KEYWORD -> {
                val stmtExpressionMarker = mark()

                // Mark statement expression keyword token with element type
                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                // Mark statement expression value or call error
                if (isTokenExpressionValue()) {
                    markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
                } else {
                    error("'$stmtName' statement expression expected")
                    success = false
                }

                stmtExpressionMarker.done(RenPyScriptElementTypes.GEN_STMT_EXPRESSION)
            }
            isTokenImageLabelCompatibleIdentifier(token) -> mark().also {
                while (isTokenImageLabelCompatibleIdentifier()) markAdvanceDone(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_IMAGE_PART)
                it.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_IMAGE)
            }
            else -> {
                if (allowSingleKeyword && (eof() || isTokenNewLine(token))) {
                    // If single keyword without any image is allowed for the statement - accept it and exit here
                    stmtMarker.done(stmtElementType)
                    return true
                }
                error("'$stmtName' statement image identifier or expression expected")
                success = false
            }
        }

        if (isTokenShowOrScenePropName()) {
            val stmtPropsListMarker = mark()

            while (true) {
                val propKeywordToken = token()

                val stmtPropMarker = mark()

                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                val stmtPropValMarker = mark()
                val currentToken = token()
                when (propKeywordToken) {
                    RenPyScriptTokenTypes.AS_KEYWORD, RenPyScriptTokenTypes.ONLAYER_KEYWORD -> {
                        if (currentToken == RenPyScriptTokenTypes.IDENTIFIER) {
                            advance()
                            stmtPropValMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                        } else {
                            if (propKeywordToken == RenPyScriptTokenTypes.AS_KEYWORD) {
                                error("A 'name' is expected as 'as' property value")
                            } else {
                                error("A 'name' of layer is expected as 'onlayer' property value")
                            }
                            stmtPropValMarker.drop()
                            success = false
                        }
                    }
                    RenPyScriptTokenTypes.AT_KEYWORD, RenPyScriptTokenTypes.BEHIND_KEYWORD -> {
                        if (currentToken == RenPyScriptTokenTypes.IDENTIFIER) {
                            advance()
                            while (token() == RenPyScriptTokenTypes.COMMA && tokenAhead() == RenPyScriptTokenTypes.IDENTIFIER) {
                                advance()
                                advance()
                            }
                            stmtPropValMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                        } else {
                            if (propKeywordToken == RenPyScriptTokenTypes.AT_KEYWORD) {
                                error("An 'expression' or a list of 'expressions' is expected as 'at' property value")
                            } else {
                                error("A 'name' or a list of 'names' is expected as 'behind' property value")
                            }
                            stmtPropValMarker.drop()
                            success = false
                        }
                    }
                    RenPyScriptTokenTypes.ZORDER_KEYWORD -> {
                        if (currentToken == RenPyScriptTokenTypes.IDENTIFIER || currentToken == RenPyScriptTokenTypes.STRING || currentToken == RenPyScriptTokenTypes.PLAIN_NUMBER) {
                            advance()
                            stmtPropValMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                        } else {
                            error("An 'integer' is expected as 'zorder' property value")
                            stmtPropValMarker.drop()
                            success = false
                        }
                    }
                    else -> {
                        error("Unexpected '$stmtName' property keyword: $propKeywordToken")
                        stmtPropValMarker.drop()
                        success = false
                    }
                }

                stmtPropMarker.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_PROP)

                if (eof() || isTokenNewLine() || !isTokenShowOrScenePropName()) break
            }

            stmtPropsListMarker.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_PROPS_LIST)
        }

        if (token() == RenPyScriptTokenTypes.WITH_KEYWORD) success = parseGenStmtWithClause(stmtName) && success

        if (token() == RenPyScriptTokenTypes.COLON) {
            markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_COLON)

            val errorMessage = "An indented ATL block is expected after '$stmtName' statement finishing colon"
            if (!isTokenNewLine()) {
                error(errorMessage)
                success = false
            } else {
                mark().also {
                    advanceToDedentOfCurrentBlockOrEof(currentIndentLevel)
                    it.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_ATL)
                }
            }
        } else {
            success = verifyTokenIsNewLineOrEof("New line is expected at the end of the '$stmtName' statement") && success
        }

        stmtMarker.done(stmtElementType)
        return success
    }

    protected open fun parseShowStatement(): Boolean = parseShowOrSceneStatement(
        RenPyScriptTokenTypes.SHOW_KEYWORD,
        false,
        RenPyScriptElementTypes.SHOW_STMT,
    )

    protected open fun parseSceneStatement(): Boolean = parseShowOrSceneStatement(
        RenPyScriptTokenTypes.SCENE_KEYWORD,
        true,
        RenPyScriptElementTypes.SCENE_STMT,
    )

    protected open fun parseWithStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.WITH_KEYWORD) {
                error("Invalid 'with' statement start token: $it")
                return false
            }
        }

        var success = true
        mark().also { stmtMarker ->

            // Mark keyword token with element type
            markAndDoneGeneralKeyword()

            val token = token()
            mark().also { valueMarker ->
                var found = false
                when {
                    token == RenPyScriptTokenTypes.NONE -> {
                        advance()
                        found = true
                    }
                    isCurrentlyAtPythonMethodCallStart() -> {
                        parsePythonMethodCall()
                        found = true
                    }
                    token == RenPyScriptTokenTypes.IDENTIFIER -> {
                        advance()
                        found = true
                    }

                    else -> {
                        error("'with' statement transition object or None expected")
                    }
                }

                if (!found) {
                    valueMarker.drop()
                    success = false
                } else {
                    valueMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                }
            }

            stmtMarker.done(RenPyScriptElementTypes.WITH_STMT)
        }

        return verifyTokenIsNewLineOrEof("New line is expected at the end of the 'with' statement") && success
    }

    protected open fun parseHideStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.HIDE_KEYWORD) {
                error("Invalid 'hide' statement start token: $it")
                return false
            }
        }

        val stmtMarker = mark()

        // Mark keyword token with element type
        markAndDoneGeneralKeyword()

        var success = true

        if (isTokenImageLabelCompatibleIdentifier()) {
            mark().also {
                while (isTokenImageLabelCompatibleIdentifier()) markAdvanceDone(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_IMAGE_PART)
                it.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_IMAGE)
            }
        } else {
            error("'hide' statement image identifier expected")
            success = false
        }

        if (isTokenHidePropName()) {
            val stmtPropsListMarker = mark()

            val propKeywordToken = token()
            if (propKeywordToken == RenPyScriptTokenTypes.ONLAYER_KEYWORD) {
                val stmtPropMarker = mark()

                markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                // 'hide' statement only has one keyword - onlayer, which only has one possible value - an identifier
                mark().also {
                    val token = token()
                    if (token == RenPyScriptTokenTypes.IDENTIFIER) {
                        advance()
                        it.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                    } else {
                        error("A 'name' of layer is expected as 'onlayer' property value")
                        it.drop()
                        success = false
                    }
                }

                stmtPropMarker.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_PROP)
            } else {
                error("Unexpected 'hide' statement property keyword: $propKeywordToken")
                success = false
            }

            stmtPropsListMarker.done(RenPyScriptElementTypes.IMAGE_DISPLAY_CONTROL_STMT_PROPS_LIST)
        }

        if (token() == RenPyScriptTokenTypes.WITH_KEYWORD) success = parseGenStmtWithClause("hide") && success

        success = verifyTokenIsNewLineOrEof("New line is expected at the end of the 'hide' statement") && success

        stmtMarker.done(RenPyScriptElementTypes.HIDE_STMT)
        return success
    }

    protected open fun parsePassStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.PASS_KEYWORD) {
                error("Invalid 'pass' statement start token: $it")
                return false
            }
        }

        mark().also { stmtMarker ->
            markAndDoneGeneralKeyword()
            stmtMarker.done(RenPyScriptElementTypes.PASS_STMT)
        }

        return verifyTokenIsNewLineOrEof("New line is expected at the end of the 'pass' statement")
    }

    protected open fun parseReturnStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.RETURN_KEYWORD) {
                error("Invalid 'return' statement start token: $it")
                return false
            }
        }

        mark().also { stmtMarker ->
            markAndDoneGeneralKeyword()

            if (!eof() && !isTokenNewLine()) {
                // Return have optional value
                mark().also { valueMarker ->
                    advanceToNewLineOrEof(error = false)
                    valueMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                }
            }

            stmtMarker.done(RenPyScriptElementTypes.RETURN_STMT)
        }

        return verifyTokenIsNewLineOrEof("New line is expected at the end of the 'return' statement")
    }

    protected open fun parseAudioControlStatement(): Boolean {
        val stmtElementType: RenPyScriptAudioControlStmtElementType = when (val stmtKeywordToken = token()) {
            RenPyScriptTokenTypes.PLAY_KEYWORD -> RenPyScriptElementTypes.PLAY_STMT
            RenPyScriptTokenTypes.STOP_KEYWORD -> RenPyScriptElementTypes.STOP_STMT
            RenPyScriptTokenTypes.QUEUE_KEYWORD -> RenPyScriptElementTypes.QUEUE_STMT
            else -> {
                this.builder.error("Invalid audio control statement keyword token: $stmtKeywordToken")
                return false
            }
        }

        val stmtMarker = mark()

        // Mark keyword token with element type
        markAndDoneGeneralKeyword()

        var success = true

        // ---------------------------------------------------------
        // NEW: Accept music, sound, and voice keywords as channels
        // ---------------------------------------------------------
        val currentToken = token()
        if (currentToken == RenPyScriptTokenTypes.IDENTIFIER ||
            currentToken == RenPyScriptTokenTypes.MUSIC_KEYWORD ||
            currentToken == RenPyScriptTokenTypes.SOUND_KEYWORD ||
            currentToken == RenPyScriptTokenTypes.VOICE_KEYWORD) {
            markAdvanceDone(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_CHANNEL)
        } else {
            error("'${stmtElementType.stmtName}' statement channel expected")
            success = false
        }

        if (stmtElementType.hasAudio) {
            mark().also {
                var localSuccess = true
                when (token()) {
                    RenPyScriptTokenTypes.SQUARE_BRACKETS_OPEN -> success = parseAudioControlStatementAudioList(stmtElementType.stmtName) && success
                    RenPyScriptTokenTypes.IDENTIFIER, RenPyScriptTokenTypes.STRING -> success = parseAudioControlStatementAudioFile(stmtElementType.stmtName) && success
                    else -> {
                        it.drop()
                        error("'${stmtElementType.stmtName}' audio file(s) expected")
                        success = false
                        localSuccess = false
                    }
                }

                if (localSuccess) it.done(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO)
            }
        }

        if (token() == RenPyScriptTokenTypes.IDENTIFIER) {
            mark().also { stmtClausesListMarker ->
                while (!eof()) {
                    if (token() != RenPyScriptTokenTypes.IDENTIFIER) break
                    mark().also { stmtClauseMarker ->
                        val clauseKeywordText = tokenText()
                        if (stmtElementType.clausesKeywords.contains(clauseKeywordText)) {
                            markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

                            if (RenPyScriptAudioControlStmtElementType.CLAUSES_WITH_VALUES.contains(clauseKeywordText)) {
                                if (isTokenAudioControlStatementClauseValue()) {
                                    markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_VALUE)
                                } else {
                                    error("'${stmtElementType.stmtName}' clause '$clauseKeywordText' expects value")
                                    success = false
                                }
                            }
                        } else {
                            if (RenPyScriptAudioControlStmtElementType.ALL_CLAUSES.contains(clauseKeywordText)) {
                                markAdvanceError("Clause '$clauseKeywordText' is not expected in audio control statement '${stmtElementType.stmtName}'")
                            } else {
                                markAdvanceError("Incorrect clause keyword '$clauseKeywordText'. Expected one of the following: ${stmtElementType.clausesKeywords}")
                            }
                            success = false
                        }
                        stmtClauseMarker.done(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_CLAUSE)
                    }
                }
                stmtClausesListMarker.done(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_CLAUSES_LIST)
            }
        }

        stmtMarker.done(stmtElementType)
        return verifyTokenIsNewLineOrEof("New line is expected at the end of the '${stmtElementType.stmtName}' audio control statement") && success
    }

    protected open fun parseAudioControlStatementAudioFile(stmtName: String): Boolean {
        val token = token()
        if (token == RenPyScriptTokenTypes.IDENTIFIER || token == RenPyScriptTokenTypes.STRING) {
            markAdvanceDone(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_FILE)
            return true
        }

        error("Invalid '$stmtName' statement audio file identifier token: $token")
        return false
    }

    protected open fun parseAudioControlStatementAudioList(stmtName: String): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.SQUARE_BRACKETS_OPEN) {
                error("Invalid '$stmtName' statement audio list start token (expected: '['): $it")
                return false
            }
        }

        val listMarker = mark()
        markAdvanceDone(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_LIST_OPEN)

        var success = true
        mark().also { marker ->
            var started = false
            var foundEndBracket = false
            outer@ while (!eof()) {
                started = true

                if (isTokenNewLine() || token() == RenPyScriptTokenTypes.INDENT) {
                    advance()
                    continue
                }

                success = parseAudioControlStatementAudioFile(stmtName) && success

                while (true) {
                    when (val token = token()) {
                        RenPyScriptTokenTypes.SQUARE_BRACKETS_CLOSE -> {
                            foundEndBracket = true
                            break@outer
                        }
                        RenPyScriptTokenTypes.COMMA -> {
                            advance()
                            break
                        }
                        else -> {
                            if (isTokenNewLine(token) || token == RenPyScriptTokenTypes.INDENT) {
                                break
                            }
                            markAdvanceError("Unexpected token inside '$stmtName' audio control statement audio list: $token")
                            success = false
                        }
                    }
                }
            }
            marker.done(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_LIST_FILES)
            success = started && foundEndBracket && success
        }

        if (token() == RenPyScriptTokenTypes.SQUARE_BRACKETS_CLOSE) {
            markAdvanceDone(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_LIST_CLOSE)
        } else {
            error("Invalid '$stmtName' statement audio list end token (expected: ']'): ${token()}")
            success = false
        }

        listMarker.done(RenPyScriptElementTypes.AUDIO_CONTROL_STMT_AUDIO_LIST)
        return success
    }

    protected open fun parsePauseStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.PAUSE_KEYWORD) {
                error("Invalid 'pause' statement start token: $it")
                return false
            }
        }

        mark().also { stmtMarker ->
            markAndDoneGeneralKeyword()

            if (!eof() && !isTokenNewLine()) {
                // Pause have optional number value
                mark().also { valueMarker ->
                    val token = token()
                    advance()
                    if (isTokenNumber(token) || token == RenPyScriptTokenTypes.IDENTIFIER) {
                        valueMarker.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
                    } else {
                        valueMarker.error("Invalid 'pause' statement value")
                    }
                }
            }

            stmtMarker.done(RenPyScriptElementTypes.PAUSE_STMT)
        }

        return verifyTokenIsNewLineOrEof("New line is expected at the end of the 'return' statement")
    }

    protected open fun parseQuickPythonStatement(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.ONE_LINE_PYTHON_STATEMENT) {
                error("Invalid one-line python statement start token: $it")
                return false
            }
        }

        markAdvanceDone(RenPyScriptElementTypes.ONE_LINE_PYTHON_STMT)

        return verifyTokenIsNewLineOrEof("New line is expected at the end of the one-line python statement")
    }

    protected fun isCurrentlyAtStartOfDialogStatementStructure(): Boolean {
        if (!isTokenDialogueStatementStart()) return false

        val step = when (tokenAhead(0)) {
            RenPyScriptTokenTypes.IDENTIFIER -> 1
            RenPyScriptTokenTypes.STRING if (isTokenDialogueStatementTextStart(tokenAhead(1))) -> 1
            RenPyScriptTokenTypes.STRING -> 0
            else -> return false
        }

        return isTokenDialogueStatementTextStart(tokenAhead(step)) && isTokenNewLine(tokenAhead(step + 1))
    }

    protected open fun isCurrentlyAtPythonMethodCallStart(): Boolean = token() == RenPyScriptTokenTypes.IDENTIFIER &&
            lookAheadForTokenAfterTokenSet(
                RenPyScriptTokenTypes.PARENTHESES_OPEN,
                RenPyScriptTokenSets.PYTHON_METHOD_CALL_NAME_PARTS
            )

    protected open fun parsePythonMethodCall(): Boolean {
        if (!isCurrentlyAtPythonMethodCallStart()) {
            error("Invalid python method call start token: ${token()}")
            return false
        }

        val pythonMethodCallMarker = mark()

        mark().also {
            while (isTokenPythonMethodCallNamePart()) advance()
            it.done(RenPyScriptElementTypes.PYTHON_METHOD_CALL_NAME)
        }

        val parseParenthesesResult = parsePythonMethodCallParentheses()

        if (!parseParenthesesResult) error("Failed to parse python method call parentheses")

        pythonMethodCallMarker.done(RenPyScriptElementTypes.PYTHON_METHOD_CALL)
        return parseParenthesesResult
    }

    protected open fun parsePythonMethodCallParentheses(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.PARENTHESES_OPEN) {
                error("Invalid python method call parenthesis open token: $it")
                return false
            }
        }

        val parenthesesMarker = mark()

        markAdvanceDone(RenPyScriptElementTypes.PYTHON_METHOD_CALL_PARENTHESES_OPEN)

        var closeFound = false
        var success = true
        mark().also {
            while (!eof()) {
                when (token()) {
                    RenPyScriptTokenTypes.PARENTHESES_CLOSE -> {
                        closeFound = true
                        break
                    }
                    RenPyScriptTokenTypes.PARENTHESES_OPEN -> success = parsePythonMethodCallParentheses() && success
                    else -> advance()
                }
            }
            it.done(RenPyScriptElementTypes.PYTHON_METHOD_CALL_PARENTHESES_CONTENT)
        }

        if (!closeFound) {
            error("Close parenthesis for python method call is not found")
        } else {
            markAdvanceDone(RenPyScriptElementTypes.PYTHON_METHOD_CALL_PARENTHESES_CLOSE)
        }

        parenthesesMarker.done(RenPyScriptElementTypes.PYTHON_METHOD_CALL_PARENTHESES)

        return closeFound && success
    }

    protected open fun parseGenStmtArgsList(): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.PARENTHESES_OPEN) {
                error("Invalid general statement arguments list parenthesis open token: $it")
                return false
            }
        }

        val genStmtArgsListMarker = mark()

        markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_ARGS_LIST_OPEN)

        var openedParentheses = 1
        mark().also {
            while (!eof()) {
                when (token()) {
                    RenPyScriptTokenTypes.PARENTHESES_CLOSE -> {
                        openedParentheses--
                        if (openedParentheses == 0) break
                        advance()
                    }
                    RenPyScriptTokenTypes.PARENTHESES_OPEN -> {
                        openedParentheses++
                        advance()
                    }
                    else -> advance()
                }
            }
            it.done(RenPyScriptElementTypes.GEN_STMT_ARGS_LIST_CONTENT)
        }

        if (openedParentheses != 0) error("Statement arguments list parentheses are not closed")
        else markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_ARGS_LIST_CLOSE)

        genStmtArgsListMarker.done(RenPyScriptElementTypes.GEN_STMT_ARGS_LIST)

        return openedParentheses == 0
    }

    protected open fun parseGenStmtWithClause(stmtName: String): Boolean {
        token().also {
            if (it != RenPyScriptTokenTypes.WITH_KEYWORD) {
                error("Invalid '$stmtName' statement 'with' clause start token: $it")
                return false
            }
        }

        val stmtWithClauseMarker = mark()

        markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

        var success = true

        mark().also {
            var failMessage: String? = null
            when {
                isCurrentlyAtPythonMethodCallStart() -> {
                    if (!parsePythonMethodCall()) failMessage = "Failed to parse '$stmtName' statement 'with' clause value python method call"
                }
                token() == RenPyScriptTokenTypes.IDENTIFIER -> advance()

                else -> failMessage = "An 'expression' is expected as '$stmtName' statement 'with' clause value"
            }

            if (failMessage != null) {
                error(failMessage)
                it.drop()
                success = false
            } else it.done(RenPyScriptElementTypes.GEN_STMT_VALUE)
        }

        stmtWithClauseMarker.done(RenPyScriptElementTypes.GEN_STMT_WITH_CLAUSE)
        return success
    }

    protected fun advance() = this.builder.advanceLexer()

    protected fun advanceToNewLineOrEof(error: Boolean = true) {
        var errorMarker: Marker? = null
        while (true) {
            if (eof() || isTokenNewLine()) {
                if (error) errorMarker?.error("Unexpected tokens")
                break
            }
            if (errorMarker == null && error) errorMarker = mark()
            advance()
        }
    }

    protected fun advanceToDedentOfCurrentBlockOrEof(exitWhenIndentEqualOrLess: Int) {
        while (!eof()) {
            val token = token()
            if (isTokenNewLine(token)) {
                val nextToken = tokenAhead(1)
                if (nextToken == null || isTokenNewLine(nextToken)) {
                    advance()
                    continue
                }

                if (nextToken == RenPyScriptTokenTypes.INDENT) {
                    val nextNextToken = tokenAhead(2)
                    if (nextNextToken == null || isTokenNewLine(nextNextToken)) {
                        advance()
                        advance()
                        continue
                    }
                }
            } else if (token == RenPyScriptTokenTypes.INDENT) {
                val nextToken = tokenAhead(1)
                if (nextToken == null || isTokenNewLine(nextToken)) {
                    advance()
                    continue
                }
            }

            if (isTokenNewLine() && getCurrentLineIndentLevel() <= exitWhenIndentEqualOrLess) break
            advance()
        }
    }

    @Suppress("SameParameterValue")
    protected fun lookAheadForTokenAfterTokenSet(expectedToken: IElementType, acceptableTokenSet: TokenSet): Boolean {
        if (acceptableTokenSet.contains(expectedToken)) {
            LOG.error("Acceptable token set '$acceptableTokenSet' contains expected token '$expectedToken'. It will cause 'false' searching till EOF")
            return false
        }

        var aheadToken: IElementType? = null
        var step = 0
        while (this.builder.lookAhead(step++).also { aheadToken = it } != null) {
            if (acceptableTokenSet.contains(aheadToken)) continue
            if (aheadToken == expectedToken) return true
            break
        }
        return false
    }

    protected fun token(): IElementType? = this.builder.tokenType

    protected fun tokenText(): String? = this.builder.tokenText

    protected fun tokenAhead(step: Int = 1): IElementType? = this.builder.lookAhead(step)

    protected fun eof(): Boolean = this.builder.eof()

    protected fun mark(): Marker = this.builder.mark()

    protected fun error(@ParsingError message: String) = this.builder.error(message)

    protected open fun markAdvanceDone(elementType: IElementType) {
        mark().also {
            advance()
            it.done(elementType)
        }
    }

    protected open fun markAdvanceError(@ParsingError message: String) {
        mark().also {
            advance()
            it.error(message)
        }
    }

    protected open fun markAndDoneGeneralKeyword() = markAdvanceDone(RenPyScriptElementTypes.GEN_STMT_KEYWORD)

    protected open fun getCurrentLineIndentLevel(): Int {
        if (eof()) return -1

        val initialToken = token()
        if (initialToken != RenPyScriptTokenTypes.INDENT && !isTokenNewLine(initialToken)) {
            var step = -1
            while (!eof()) {
                val token = this.builder.rawLookup(step--)

                if (token == RenPyScriptTokenTypes.INDENT) return rawTokenText(step + 1)?.length ?: 0

                if (token == null || isTokenNewLine(token)) return 0
            }
        }

        var latestIndentLevel = 0
        var step = 0
        while (!eof()) {
            val currentStep = step++
            val token = this.builder.rawLookup(currentStep)
            when (token) {
                null -> break
                RenPyScriptTokenTypes.INDENT -> {
                    latestIndentLevel = rawTokenText(currentStep)?.length ?: 0
                    continue
                }
                RenPyScriptTokenTypes.NEW_LINE -> {
                    latestIndentLevel = 0
                    continue
                }
                else -> {
                    return latestIndentLevel
                }
            }
        }
        return -1
    }

    protected open fun rawTokenText(step: Int): String? {
        if (eof()) return null
        return try {
            this.builder.originalText.subSequence(this.builder.rawTokenTypeStart(step), this.builder.rawTokenTypeStart(step + 1)).toString()
        } catch (_: Exception) {
            null
        }
    }

    protected open fun isTokenBlockStatementStart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.BLOCK_STATEMENT_STARTS.contains(token ?: token())

    protected open fun isTokenDialogueStatementStart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.DIALOGUE_STATEMENT_STARTS.contains(token ?: token())

    protected open fun isTokenMenuStatementChoiceCaption(token: IElementType? = null): Boolean = RenPyScriptTokenSets.MENU_CHOICE_CAPTIONS.contains(token ?: token())

    protected open fun isTokenDialogueStatementTextStart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.DIALOGUE_STATEMENT_TEXTS.contains(token ?: token())

    protected open fun isTokenExpressionValue(token: IElementType? = null): Boolean = RenPyScriptTokenSets.EXPRESSION_VALUES.contains(token ?: token())

    protected open fun isTokenImageDisplayControlStatementStart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.IMAGE_DISPLAY_CONTROL_STATEMENT_STARTS.contains(token ?: token())

    protected open fun isTokenAudioControlStatementStart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.AUDIO_CONTROL_STATEMENT_STARTS.contains(token ?: token())

    protected open fun isTokenAudioControlStatementClauseValue(token: IElementType? = null): Boolean = RenPyScriptTokenSets.AUDIO_CONTROL_STATEMENT_CLAUSE_VALUES.contains(token ?: token())

    protected open fun isTokenNumber(token: IElementType? = null): Boolean = RenPyScriptTokenSets.NUMBERS.contains(token ?: token())

    protected open fun isTokenShowOrScenePropName(token: IElementType? = null): Boolean = RenPyScriptTokenSets.SHOW_OR_SCENE_PARAM_KEYWORDS.contains(token ?: token())

    protected open fun isTokenHidePropName(token: IElementType? = null): Boolean = RenPyScriptTokenSets.HIDE_PARAM_KEYWORDS.contains(token ?: token())

    protected open fun isTokenNewLine(token: IElementType? = null): Boolean = RenPyScriptTokenSets.NEW_LINES.contains(token ?: token())

    protected open fun isTokenIndent(token: IElementType? = null): Boolean = RenPyScriptTokenTypes.INDENT == (token ?: token())

    protected open fun isTokenImageLabelCompatibleIdentifier(token: IElementType? = null): Boolean = RenPyScriptTokenSets.IMAGE_LABEL_COMPATIBLE_IDENTIFIERS.contains(token ?: token())

    protected open fun isTokenPythonMethodCallNamePart(token: IElementType? = null): Boolean = RenPyScriptTokenSets.PYTHON_METHOD_CALL_NAME_PARTS.contains(token ?: token())

    protected open fun verifyTokenIsNewLineOrEof(@ParsingError errorMessage: String): Boolean {
        if (eof() || isTokenNewLine()) return true
        error(errorMessage)
        return false
    }
}