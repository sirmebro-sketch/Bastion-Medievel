package de.bastion.medieval.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import de.bastion.medieval.GameUiState
import de.bastion.medieval.engine.Language
import de.bastion.medieval.engine.Paragraph
import de.bastion.medieval.engine.Suggestion

/**
 * The whole game on one screen: a leather frame with the current place on top, the story
 * on a parchment page, and suggestions plus free text input at the bottom.
 */
@Composable
fun GameScreen(
    state: GameUiState,
    onSubmit: (String) -> Unit,
    onLanguage: (Language) -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
    onShowSheet: () -> Unit = {},
    onShowCredits: () -> Unit = {},
    onShowRolls: (Boolean) -> Unit = {},
) {
    val strings = UiStrings.of(state.language)
    var input by rememberSaveable { mutableStateOf("") }
    var confirmNewGame by remember { mutableStateOf(false) }

    fun send() {
        val text = input.trim()
        if (text.isNotEmpty()) {
            onSubmit(text)
            input = ""
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Leather)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Header(
            state = state,
            strings = strings,
            actions = MenuActions(
                onLanguage = onLanguage,
                onHelp = { onSubmit(strings.helpCommand) },
                onNewGame = { confirmNewGame = true },
                onShowSheet = onShowSheet,
                onShowCredits = onShowCredits,
                onShowRolls = onShowRolls,
            ),
        )
        StoryLog(state, Modifier.weight(1f))
        Controls(
            suggestions = state.suggestions,
            input = input,
            onInputChange = { input = it },
            onSend = ::send,
            onSuggestion = onSubmit,
            strings = strings,
        )
    }

    if (confirmNewGame) {
        AlertDialog(
            onDismissRequest = { confirmNewGame = false },
            confirmButton = {
                TextButton(onClick = {
                    confirmNewGame = false
                    onNewGame()
                }) { Text(strings.newGameConfirm, color = Palette.Burgundy) }
            },
            dismissButton = {
                TextButton(onClick = { confirmNewGame = false }) { Text(strings.cancel, color = Palette.InkMuted) }
            },
            title = { Text(strings.newGameTitle, fontFamily = Fonts.Cinzel, fontWeight = FontWeight.Bold) },
            text = { Text(strings.newGameText) },
            containerColor = Palette.ParchmentLight,
        )
    }
}

private class MenuActions(
    val onLanguage: (Language) -> Unit,
    val onHelp: () -> Unit,
    val onNewGame: () -> Unit,
    val onShowSheet: () -> Unit,
    val onShowCredits: () -> Unit,
    val onShowRolls: (Boolean) -> Unit,
)

@Composable
private fun Header(state: GameUiState, strings: UiStrings, actions: MenuActions) {
    val language = state.language
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .background(LeatherBrush)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Bastion Medieval",
                style = TextStyle(fontFamily = Fonts.Cinzel, fontSize = 11.sp, letterSpacing = 3.sp, color = Palette.GoldDim),
            )
            Text(
                state.locationName,
                style = TextStyle(fontFamily = Fonts.Cinzel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Gold),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
        }
        Box(Modifier.align(Alignment.CenterEnd)) {
            MenuButton(strings.menu) { menuOpen = true }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = Palette.ParchmentLight,
            ) {
                CheckedItem(strings.languageGerman, checked = language == Language.DE) {
                    menuOpen = false
                    actions.onLanguage(Language.DE)
                }
                CheckedItem(strings.languageEnglish, checked = language == Language.EN) {
                    menuOpen = false
                    actions.onLanguage(Language.EN)
                }
                HorizontalDivider(color = Palette.GoldDim.copy(alpha = 0.5f))
                if (!state.inCreation) {
                    DropdownMenuItem(text = { Text(strings.sheet) }, onClick = {
                        menuOpen = false
                        actions.onShowSheet()
                    })
                }
                CheckedItem(strings.showRolls, checked = state.showRolls) {
                    menuOpen = false
                    actions.onShowRolls(!state.showRolls)
                }
                DropdownMenuItem(text = { Text(strings.help) }, onClick = {
                    menuOpen = false
                    actions.onHelp()
                })
                DropdownMenuItem(text = { Text(strings.credits) }, onClick = {
                    menuOpen = false
                    actions.onShowCredits()
                })
                HorizontalDivider(color = Palette.GoldDim.copy(alpha = 0.5f))
                DropdownMenuItem(text = { Text(strings.newGame) }, onClick = {
                    menuOpen = false
                    actions.onNewGame()
                })
            }
        }
    }
    GoldRule()
}

@Composable
private fun CheckedItem(label: String, checked: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                (if (checked) "✓  " else "     ") + label,
                fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        onClick = onClick,
    )
}

/** Three gold dots. */
@Composable
private fun MenuButton(description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val radius = 2.dp.toPx()
            for (i in 0..2) {
                drawCircle(Palette.Gold, radius, Offset(size.width / 2, size.height * (0.2f + 0.3f * i)))
            }
        }
    }
}

@Composable
private fun StoryLog(state: GameUiState, modifier: Modifier) {
    val listState = rememberLazyListState()
    val entries = if (state.showRolls) state.log else state.log.filter { it.paragraph.kind != Paragraph.Kind.ROLL }
    LaunchedEffect(state.latestAnswerId) {
        if (entries.isEmpty()) return@LaunchedEffect
        val target = entries.indexOfFirst { it.id == state.latestAnswerId }
        if (target >= 0) listState.animateScrollToItem(target) else listState.scrollToItem(entries.lastIndex)
    }
    val locale = LocaleList(state.language.code)
    Box(
        modifier
            .fillMaxWidth()
            .parchment(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("story"),
        ) {
            items(entries, key = { it.id }) { entry ->
                LogParagraph(
                    entry.paragraph,
                    locale,
                    Modifier.animateItem(fadeInSpec = tween(durationMillis = 450), placementSpec = null, fadeOutSpec = null),
                )
            }
        }
    }
}

private val BodyStyle = TextStyle(
    fontFamily = Fonts.Garamond,
    fontSize = 18.sp,
    lineHeight = 26.sp,
    color = Palette.Ink,
    textAlign = TextAlign.Justify,
    hyphens = Hyphens.Auto,
    lineBreak = LineBreak.Paragraph,
)
private val SceneStyle = BodyStyle.copy(fontSize = 19.sp, lineHeight = 28.sp)
private val DialogueStyle = BodyStyle.copy(fontStyle = FontStyle.Italic)
private val HintStyle = TextStyle(
    fontFamily = Fonts.Garamond,
    fontSize = 15.sp,
    lineHeight = 21.sp,
    fontStyle = FontStyle.Italic,
    color = Palette.InkMuted,
)
private val InputStyle = TextStyle(fontFamily = Fonts.Garamond, fontSize = 17.sp, fontStyle = FontStyle.Italic, color = Palette.BurgundyDark)
private val TitleStyle = TextStyle(
    fontFamily = Fonts.Cinzel,
    fontWeight = FontWeight.Bold,
    fontSize = 21.sp,
    lineHeight = 28.sp,
    letterSpacing = 1.sp,
    color = Palette.BurgundyDark,
    textAlign = TextAlign.Center,
)

@Composable
private fun LogParagraph(paragraph: Paragraph, locale: LocaleList, modifier: Modifier) {
    when (paragraph.kind) {
        Paragraph.Kind.INPUT -> Row(modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp)) {
            Text("›", style = InputStyle.copy(fontStyle = FontStyle.Normal, color = Palette.Burgundy), modifier = Modifier.padding(end = 8.dp))
            Text(paragraph.text, style = InputStyle)
        }
        Paragraph.Kind.TITLE -> Column(
            modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(paragraph.text, style = TitleStyle.copy(localeList = locale), modifier = Modifier.semantics { heading() })
            Ornament(Modifier.padding(top = 6.dp).width(150.dp).height(9.dp))
        }
        Paragraph.Kind.SCENE -> Text(
            illuminated(paragraph.text),
            style = SceneStyle.copy(localeList = locale),
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        Paragraph.Kind.TEXT -> Text(
            paragraph.text,
            style = BodyStyle.copy(localeList = locale),
            modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        )
        Paragraph.Kind.DIALOGUE -> Text(
            paragraph.text,
            style = DialogueStyle.copy(localeList = locale),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .drawBehind {
                    drawLine(Palette.GoldDim, Offset(0f, 4.dp.toPx()), Offset(0f, size.height - 4.dp.toPx()), 2.5.dp.toPx())
                }
                .padding(start = 14.dp),
        )
        Paragraph.Kind.HINT -> Text(
            paragraph.text,
            style = HintStyle.copy(localeList = locale),
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        )
        Paragraph.Kind.OPTION -> Text(
            option(paragraph.text),
            style = BodyStyle.copy(fontSize = 17.sp, lineHeight = 24.sp, textAlign = TextAlign.Start, localeList = locale),
            modifier = modifier.fillMaxWidth().padding(vertical = 3.dp),
        )
        Paragraph.Kind.ROLL -> RollLine(paragraph.text, modifier)
        Paragraph.Kind.TABLE -> Table(paragraph.text, modifier)
    }
}

/** "Name — description": the name in semi-bold burgundy. */
private fun option(text: String): AnnotatedString = buildAnnotatedString {
    val split = text.indexOf(" — ")
    if (split < 0) {
        append(text)
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = Palette.BurgundyDark)) { append(text.substring(0, split)) }
    append(text.substring(split))
}

/** A dice roll on a small tag with a d20 mark. */
@Composable
private fun RollLine(text: String, modifier: Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .background(Palette.ParchmentEdge.copy(alpha = 0.35f))
            .border(1.dp, Palette.GoldDim.copy(alpha = 0.7f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        D20Mark(Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = TextStyle(fontFamily = Fonts.Garamond, fontSize = 15.sp, lineHeight = 20.sp, color = Palette.BurgundyDark))
    }
}

/** A small twenty-sided die: a hexagon with an inner triangle. */
@Composable
private fun D20Mark(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.3.dp.toPx())
        val hexagon = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, h * 0.25f)
            lineTo(w, h * 0.75f)
            lineTo(w * 0.5f, h)
            lineTo(0f, h * 0.75f)
            lineTo(0f, h * 0.25f)
            close()
        }
        val triangle = Path().apply {
            moveTo(w * 0.5f, h * 0.22f)
            lineTo(w * 0.82f, h * 0.7f)
            lineTo(w * 0.18f, h * 0.7f)
            close()
        }
        drawPath(hexagon, Palette.Burgundy, style = stroke)
        drawPath(triangle, Palette.Burgundy, style = stroke)
    }
}

/**
 * Rows separated by line breaks, columns by tabs. Tables with three or more columns
 * treat the first row as a header; two-column tables are key/value lists.
 */
@Composable
private fun Table(text: String, modifier: Modifier) {
    val rows = text.split('\n').map { it.split('\t') }
    val columns = rows.maxOfOrNull { it.size } ?: 0
    val header = columns >= 3
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, Palette.GoldDim.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        rows.forEachIndexed { index, cells ->
            val isHeader = header && index == 0
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                cells.forEachIndexed { column, cell ->
                    val first = column == 0
                    Text(
                        cell,
                        style = TextStyle(
                            fontFamily = if (isHeader) Fonts.Cinzel else Fonts.Garamond,
                            fontSize = if (isHeader) 12.sp else 16.sp,
                            fontWeight = if (!first && !isHeader && columns == 2) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isHeader || (first && columns == 2)) Palette.InkMuted else Palette.Ink,
                            textAlign = if (first) TextAlign.Start else TextAlign.End,
                        ),
                        modifier = Modifier.weight(
                            when {
                                columns == 2 -> if (first) 1f else 1.5f
                                first -> 1.8f
                                else -> 1f
                            },
                        ),
                    )
                }
            }
            if (isHeader) HorizontalDivider(color = Palette.GoldDim.copy(alpha = 0.5f))
        }
    }
}

/** Sets the first letter of a description as a blackletter initial. */
private fun illuminated(text: String): AnnotatedString = buildAnnotatedString {
    if (text.isEmpty() || !text[0].isLetter()) {
        append(text)
        return@buildAnnotatedString
    }
    withStyle(SpanStyle(fontFamily = Fonts.Fraktur, fontSize = 1.5.em, color = Palette.Burgundy)) {
        append(text[0])
    }
    append(text.substring(1))
}

@Composable
private fun Controls(
    suggestions: List<Suggestion>,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    strings: UiStrings,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(LeatherBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)),
    ) {
        GoldRule()
        if (suggestions.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .testTag("suggestions"),
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(suggestion.label) { onSuggestion(suggestion.command) }
                }
            }
        }
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InputField(input, onInputChange, onSend, strings.inputPlaceholder, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            SealButton(strings.send, onSend)
        }
    }
}

@Composable
private fun SuggestionChip(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        Modifier
            .clip(shape)
            .background(Palette.Parchment)
            .border(1.dp, Palette.GoldDim, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, style = TextStyle(fontFamily = Fonts.Garamond, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Palette.Ink))
    }
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    placeholder: String,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val textStyle = TextStyle(fontFamily = Fonts.Garamond, fontSize = 18.sp, color = Palette.Ink)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clip(shape)
            .background(Palette.ParchmentLight)
            .border(1.dp, Palette.GoldDim, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("input"),
        textStyle = textStyle,
        singleLine = true,
        cursorBrush = SolidColor(Palette.Burgundy),
        // Commands are short words; autocorrect would only get in the way.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(fontStyle = FontStyle.Italic, color = Palette.InkMuted))
                }
                innerTextField()
            }
        },
    )
}

/** A round wax seal with an arrow: sends the typed command. */
@Composable
private fun SealButton(description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Palette.Burgundy, Palette.BurgundyDark)))
            .border(1.5.dp, Palette.GoldDim, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            val arrow = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.5f)
                lineTo(size.width * 0.85f, size.height * 0.5f)
                moveTo(size.width * 0.55f, size.height * 0.2f)
                lineTo(size.width * 0.85f, size.height * 0.5f)
                lineTo(size.width * 0.55f, size.height * 0.8f)
            }
            drawPath(arrow, Palette.ParchmentLight, style = stroke)
        }
    }
}
