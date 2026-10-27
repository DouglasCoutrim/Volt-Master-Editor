package com.example

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricGreenBright
import com.example.ui.theme.ElectricGreenDark
import com.example.ui.theme.ElectricNeonGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.MetallicSilverDark
import com.example.ui.theme.MetallicSilverLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VoltMasterTheme
import kotlinx.coroutines.launch

// ==========================================
// CONSTANTS & MEMORY OFFSETS
// ==========================================
const val MIN_DUMP_SIZE = 0x0250 // 592 bytes minimum

// Exact offsets requested
const val OFFSET_SN1 = 0x009C // 156: Primary Serial Number (14 bytes, ASCII null-padded)
const val LEN_SN1 = 14

const val OFFSET_SN2 = 0x011E // 286: Secondary Serial Number (15 bytes, ASCII null-padded)
const val LEN_SN2 = 15

const val OFFSET_PRIMARY_CYCLES = 0x01A6 // 422: Primary Cycles (UInt16 LE, 2 bytes)
const val OFFSET_PRIMARY_REF = 0x01A8    // 424: Primary Reference Cycle (UInt16 LE, Current + 1)

const val OFFSET_MIRROR_CYCLES = 0x0226  // 550: Mirror Cycles (UInt16 LE, 2 bytes)
const val OFFSET_MIRROR_REF = 0x0228     // 552: Mirror Reference Cycle (UInt16 LE, Current + 1)

// ==========================================
// BYTE MANIPULATION HELPER FUNCTIONS
// ==========================================

/**
 * Reads a 16-bit unsigned integer (UInt16) in Little Endian format.
 * Low byte at `offset`, High byte at `offset + 1`.
 */
fun readUInt16LE(bytes: ByteArray, offset: Int): Int {
    if (offset + 1 >= bytes.size) return 0
    val low = bytes[offset].toInt() and 0xFF
    val high = bytes[offset + 1].toInt() and 0xFF
    return low or (high shl 8)
}

/**
 * Writes a 16-bit unsigned integer (UInt16) in Little Endian format.
 * Low byte at `offset`, High byte at `offset + 1`.
 */
fun writeUInt16LE(bytes: ByteArray, offset: Int, value: Int) {
    if (offset + 1 >= bytes.size) return
    val clamped = value.coerceIn(0, 0xFFFF)
    bytes[offset] = (clamped and 0xFF).toByte()
    bytes[offset + 1] = ((clamped ushr 8) and 0xFF).toByte()
}

/**
 * Formats a 16-bit integer into a Little Endian hex string: "LowByte HighByte"
 * e.g., 25 (0x0019) -> "19 00"
 */
fun formatUInt16LEHex(value: Int): String {
    val clamped = value.coerceIn(0, 0xFFFF)
    val low = clamped and 0xFF
    val high = (clamped ushr 8) and 0xFF
    return String.format("%02X %02X", low, high)
}

/**
 * Reads an ASCII string up to `maxLength` bytes, stopping at the first null (0x00) byte.
 */
fun readAsciiString(bytes: ByteArray, offset: Int, maxLength: Int): String {
    if (offset >= bytes.size) return ""
    val limit = (offset + maxLength).coerceAtMost(bytes.size)
    var nullIndex = limit
    for (i in offset until limit) {
        if (bytes[i] == 0.toByte()) {
            nullIndex = i
            break
        }
    }
    val slice = bytes.copyOfRange(offset, nullIndex)
    return String(slice, Charsets.US_ASCII).filter { it.code in 32..126 }
}

/**
 * Writes an ASCII string into `bytes` starting at `offset`, truncated to `maxLength`
 * and null-padded (0x00) for all remaining bytes.
 */
fun writeAsciiString(bytes: ByteArray, offset: Int, maxLength: Int, text: String) {
    val asciiBytes = text.take(maxLength).toByteArray(Charsets.US_ASCII)
    for (i in 0 until maxLength) {
        val targetIdx = offset + i
        if (targetIdx < bytes.size) {
            bytes[targetIdx] = if (i < asciiBytes.size) asciiBytes[i] else 0.toByte()
        }
    }
}

/**
 * Formats a slice of bytes into readable hex string e.g. "01 A6 3F ..."
 */
fun formatHexSlice(bytes: ByteArray, offset: Int, length: Int): String {
    if (offset >= bytes.size) return ""
    val end = (offset + length).coerceAtMost(bytes.size)
    return bytes.sliceArray(offset until end).joinToString(" ") { String.format("%02X", it) }
}

/**
 * Auto-detects if the input bytes represent space-separated (or whitespace-separated)
 * ASCII hex pairs (e.g. "01 00 00 00..."). If so, converts to raw ByteArray.
 * Returns null if the content is already raw binary.
 */
fun detectAndParseHexText(inputBytes: ByteArray): ByteArray? {
    return try {
        val text = String(inputBytes, Charsets.UTF_8).trim()
        if (text.isEmpty()) return null

        // Check for ASCII hex tokens (at least 32 tokens to consider it a valid hex dump)
        val tokens = text.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }
        if (tokens.size < 32) return null

        // Validate that first 100 tokens are valid hex byte representations (1 or 2 hex digits)
        val sampleSize = tokens.size.coerceAtMost(128)
        val isAllHex = tokens.take(sampleSize).all { token ->
            val clean = token.removePrefix("0x").removePrefix("0X")
            clean.length in 1..2 && clean.all { it in "0123456789abcdefABCDEF" }
        }

        if (!isAllHex) return null

        // Parse all tokens into ByteArray
        val output = ByteArray(tokens.size)
        for (i in tokens.indices) {
            val clean = tokens[i].removePrefix("0x").removePrefix("0X")
            output[i] = clean.toInt(16).toByte()
        }
        output
    } catch (_: Exception) {
        null
    }
}

/**
 * Converts a ByteArray into formatted space-separated ASCII hex text (16 bytes per line).
 */
fun byteArrayToHexText(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (i in bytes.indices) {
        sb.append(String.format("%02X", bytes[i]))
        if (i < bytes.size - 1) {
            if ((i + 1) % 16 == 0) {
                sb.append("\n")
            } else {
                sb.append(" ")
            }
        }
    }
    return sb.toString()
}

/**
 * Generates a realistic mock EEPROM dump (1024 bytes) for instant testing.
 */
fun createDemoDump(): ByteArray {
    val demo = ByteArray(0x0400) // 1024 bytes

    // Fill with default background pattern
    for (i in demo.indices) {
        demo[i] = if (i % 2 == 0) 0xFF.toByte() else 0x00.toByte()
    }

    // Set Header
    "VOLTMASTER_EEPROM".toByteArray(Charsets.US_ASCII).copyInto(demo, 0x0000)

    // Primary Serial (0x009C, 14 bytes)
    writeAsciiString(demo, OFFSET_SN1, LEN_SN1, "VM-BAT-202401")

    // Secondary Serial (0x011E, 15 bytes)
    writeAsciiString(demo, OFFSET_SN2, LEN_SN2, "VM-6S-5200-PRO")

    // Primary Block (Cycles: 35, Ref: 36)
    writeUInt16LE(demo, OFFSET_PRIMARY_CYCLES, 35)
    writeUInt16LE(demo, OFFSET_PRIMARY_REF, 36)

    // Mirror Block (Cycles: 35, Ref: 36)
    writeUInt16LE(demo, OFFSET_MIRROR_CYCLES, 35)
    writeUInt16LE(demo, OFFSET_MIRROR_REF, 36)

    return demo
}

// ==========================================
// DATA STATE MODEL
// ==========================================
data class LoadedDumpState(
    val rawBytes: ByteArray,
    val fileName: String,
    val fileSize: Int,
    val isHexTextOrigin: Boolean,
    val originalSn1: String,
    val originalSn2: String,
    val originalCycles: Int,
    val originalRefCycles: Int,
    val mirrorCycles: Int,
    val mirrorRefCycles: Int
) {
    val isMirrorSynced: Boolean = originalCycles == mirrorCycles && originalRefCycles == mirrorRefCycles

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as LoadedDumpState
        return fileName == other.fileName && rawBytes.contentEquals(other.rawBytes)
    }

    override fun hashCode(): Int {
        var result = rawBytes.contentHashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}

// ==========================================
// MAIN ACTIVITY
// ==========================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoltMasterTheme {
                VoltMasterEditorScreen()
            }
        }
    }
}

// ==========================================
// MAIN COMPOSABLE SCREEN
// ==========================================
@Composable
fun VoltMasterEditorScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State of loaded dump
    var loadedDump by remember { mutableStateOf<LoadedDumpState?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form editing states
    var editSn1 by remember { mutableStateOf("") }
    var editSn2 by remember { mutableStateOf("") }
    var editCyclesText by remember { mutableStateOf("") }

    // Export format toggle: true = Binary (.bin), false = Hex Text (.txt)
    var exportAsBinary by remember { mutableStateOf(true) }

    // Collapsible memory offset inspection card
    var isOffsetInspectorOpen by remember { mutableStateOf(false) }

    // SAF File Open Launcher
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "dump_eeprom.bin"
                var fileSize = 0

                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado.")

                fileSize = inputBytes.size

                // Auto-detect format: Hex Text vs Raw Binary
                val detectedHexBytes = detectAndParseHexText(inputBytes)
                val workingBytes = detectedHexBytes ?: inputBytes
                val isHexOrigin = detectedHexBytes != null

                // Validate minimum size: 0x0250 (592 bytes)
                if (workingBytes.size < MIN_DUMP_SIZE) {
                    errorMessage = "Arquivo inválido: tamanho mínimo de 0x0250 (592 bytes) requerido. Tamanho do dump: ${workingBytes.size} bytes."
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(errorMessage ?: "Tamanho de arquivo insuficiente.")
                    }
                    return@rememberLauncherForActivityResult
                }

                // Parse fields
                val sn1 = readAsciiString(workingBytes, OFFSET_SN1, LEN_SN1)
                val sn2 = readAsciiString(workingBytes, OFFSET_SN2, LEN_SN2)
                val primaryCycles = readUInt16LE(workingBytes, OFFSET_PRIMARY_CYCLES)
                val primaryRef = readUInt16LE(workingBytes, OFFSET_PRIMARY_REF)
                val mirrorCycles = readUInt16LE(workingBytes, OFFSET_MIRROR_CYCLES)
                val mirrorRef = readUInt16LE(workingBytes, OFFSET_MIRROR_REF)

                loadedDump = LoadedDumpState(
                    rawBytes = workingBytes.clone(),
                    fileName = fileName,
                    fileSize = fileSize,
                    isHexTextOrigin = isHexOrigin,
                    originalSn1 = sn1,
                    originalSn2 = sn2,
                    originalCycles = primaryCycles,
                    originalRefCycles = primaryRef,
                    mirrorCycles = mirrorCycles,
                    mirrorRefCycles = mirrorRef
                )

                // Populate edit fields
                editSn1 = sn1
                editSn2 = sn2
                editCyclesText = primaryCycles.toString()
                errorMessage = null

                coroutineScope.launch {
                    val formatMsg = if (isHexOrigin) "Hex ASCII detectado e convertido (${workingBytes.size} bytes)" else "Binário RAW carregado (${workingBytes.size} bytes)"
                    snackbarHostState.showSnackbar("Arquivo carregado com sucesso: $formatMsg")
                }
            } catch (e: Exception) {
                errorMessage = "Erro ao processar arquivo: ${e.localizedMessage}"
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Erro: ${e.localizedMessage}")
                }
            }
        }
    }

    // SAF File Create/Export Launcher
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (exportAsBinary) "application/octet-stream" else "text/plain"
        )
    ) { uri: Uri? ->
        if (uri != null && loadedDump != null) {
            try {
                val base = loadedDump!!.rawBytes.clone()

                // Apply edits to byte array
                writeAsciiString(base, OFFSET_SN1, LEN_SN1, editSn1)
                writeAsciiString(base, OFFSET_SN2, LEN_SN2, editSn2)

                val newCycles = editCyclesText.toIntOrNull()?.coerceIn(0, 0xFFFF) ?: 0
                val refCycles = (newCycles + 1) and 0xFFFF

                // Write Primary Block
                writeUInt16LE(base, OFFSET_PRIMARY_CYCLES, newCycles)
                writeUInt16LE(base, OFFSET_PRIMARY_REF, refCycles)

                // Write Mirror Block
                writeUInt16LE(base, OFFSET_MIRROR_CYCLES, newCycles)
                writeUInt16LE(base, OFFSET_MIRROR_REF, refCycles)

                // Format data for saving
                val bytesToWrite: ByteArray = if (exportAsBinary) {
                    base
                } else {
                    byteArrayToHexText(base).toByteArray(Charsets.UTF_8)
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytesToWrite)
                    outputStream.flush()
                }

                // Update current loaded state
                loadedDump = loadedDump!!.copy(
                    rawBytes = base,
                    originalSn1 = editSn1,
                    originalSn2 = editSn2,
                    originalCycles = newCycles,
                    originalRefCycles = refCycles,
                    mirrorCycles = newCycles,
                    mirrorRefCycles = refCycles
                )

                val ext = if (exportAsBinary) ".bin" else ".txt"
                Toast.makeText(context, "Arquivo exportado com sucesso ($ext)!", Toast.LENGTH_LONG).show()
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Dump gravado e exportado com sucesso!")
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Falha ao exportar: ${e.localizedMessage}")
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DeepBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Constrain content width for tablet/landscape readability
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. BRAND HEADER SECTION
                BrandHeaderSection()

                // 2. FILE LOADER & DEMO SECTION
                FileLoaderCard(
                    loadedDump = loadedDump,
                    onOpenFile = {
                        openDocumentLauncher.launch(arrayOf("*/*"))
                    },
                    onLoadDemo = {
                        val demoBytes = createDemoDump()
                        val sn1 = readAsciiString(demoBytes, OFFSET_SN1, LEN_SN1)
                        val sn2 = readAsciiString(demoBytes, OFFSET_SN2, LEN_SN2)
                        val cycles = readUInt16LE(demoBytes, OFFSET_PRIMARY_CYCLES)
                        val ref = readUInt16LE(demoBytes, OFFSET_PRIMARY_REF)

                        loadedDump = LoadedDumpState(
                            rawBytes = demoBytes,
                            fileName = "voltmaster_sample_demo.bin",
                            fileSize = demoBytes.size,
                            isHexTextOrigin = false,
                            originalSn1 = sn1,
                            originalSn2 = sn2,
                            originalCycles = cycles,
                            originalRefCycles = ref,
                            mirrorCycles = cycles,
                            mirrorRefCycles = ref
                        )
                        editSn1 = sn1
                        editSn2 = sn2
                        editCyclesText = cycles.toString()
                        errorMessage = null

                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Dump de teste carregado (1024 bytes). Pronto para edição.")
                        }
                    }
                )

                // Error Banner if present
                if (errorMessage != null) {
                    ErrorBanner(errorMessage!!)
                }

                // 3. CURRENT DUMP READOUT CARD (Dashboard)
                CurrentDumpReadoutCard(loadedDump = loadedDump)

                // 4. EDIT PARAMETERS FORM CARD
                EditParametersCard(
                    isEnabled = loadedDump != null,
                    sn1 = editSn1,
                    onSn1Change = { if (it.length <= LEN_SN1) editSn1 = it },
                    sn2 = editSn2,
                    onSn2Change = { if (it.length <= LEN_SN2) editSn2 = it },
                    cyclesText = editCyclesText,
                    onCyclesTextChange = { input ->
                        // Only numeric and clamp to 65535
                        val digitsOnly = input.filter { it.isDigit() }
                        val num = digitsOnly.toIntOrNull()
                        if (digitsOnly.isEmpty()) {
                            editCyclesText = ""
                        } else if (num != null && num in 0..65535) {
                            editCyclesText = digitsOnly
                        }
                    },
                    onResetToZero = { editCyclesText = "0" },
                    onIncrementCycles = { inc ->
                        val current = editCyclesText.toIntOrNull() ?: 0
                        val target = (current + inc).coerceIn(0, 65535)
                        editCyclesText = target.toString()
                    },
                    onRestoreOriginal = {
                        loadedDump?.let {
                            editSn1 = it.originalSn1
                            editSn2 = it.originalSn2
                            editCyclesText = it.originalCycles.toString()
                        }
                    }
                )

                // 5. MEMORY OFFSET INSPECTOR (Collapsible)
                if (loadedDump != null) {
                    MemoryInspectorCard(
                        isOpen = isOffsetInspectorOpen,
                        onToggle = { isOffsetInspectorOpen = !isOffsetInspectorOpen },
                        dump = loadedDump!!,
                        previewSn1 = editSn1,
                        previewSn2 = editSn2,
                        previewCycles = editCyclesText.toIntOrNull() ?: 0
                    )
                }

                // 6. EXPORT CARD & BUTTON
                ExportSection(
                    isEnabled = loadedDump != null && (editCyclesText.toIntOrNull() != null),
                    exportAsBinary = exportAsBinary,
                    onFormatToggle = { exportAsBinary = it },
                    onExport = {
                        val defaultName = if (exportAsBinary) "voltmaster_editado.bin" else "voltmaster_editado.txt"
                        createDocumentLauncher.launch(defaultName)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ==========================================
// 1. BRAND HEADER COMPONENT
// ==========================================
@Composable
fun BrandHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Metallic Shield with Neon Green Lightning Bolt
        VoltMasterShieldLogo(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 12.dp, shape = CircleShape, spotColor = ElectricGreenBright)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Title: "VOLT MASTER - EDITOR" (Metallic silver & white)
        Text(
            text = "VOLT MASTER - EDITOR",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            color = MetallicSilverLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle: "ENERGIA PARA VOAR E MOVER" (Electric green tracking)
        Text(
            text = "ENERGIA PARA VOAR E MOVER",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                fontSize = 11.sp
            ),
            color = ElectricGreenBright,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Custom Canvas drawing a metallic silver shield with a neon green lightning bolt.
 */
@Composable
fun VoltMasterShieldLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Shield Outer Path
        val shieldPath = Path().apply {
            moveTo(w * 0.18f, h * 0.16f)
            // Top arched line
            quadraticBezierTo(w * 0.50f, h * 0.10f, w * 0.82f, h * 0.16f)
            // Right downward line
            lineTo(w * 0.82f, h * 0.52f)
            // Bottom curved point
            cubicTo(w * 0.82f, h * 0.74f, w * 0.55f, h * 0.88f, w * 0.50f, h * 0.94f)
            cubicTo(w * 0.45f, h * 0.88f, w * 0.18f, h * 0.74f, w * 0.18f, h * 0.52f)
            close()
        }

        // Fill Shield with dark metallic gradient
        val shieldFillBrush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2C2C2E), Color(0xFF141414), Color(0xFF0D0D0D))
        )
        drawPath(path = shieldPath, brush = shieldFillBrush)

        // Shield Metallic Border Stroke
        val metallicBorderBrush = Brush.linearGradient(
            colors = listOf(
                MetallicSilverLight,
                MetallicSilver,
                Color(0xFF555555),
                MetallicSilver,
                Color.White
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawPath(
            path = shieldPath,
            brush = metallicBorderBrush,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Subtle Inner Rim
        val innerShieldPath = Path().apply {
            moveTo(w * 0.24f, h * 0.22f)
            quadraticBezierTo(w * 0.50f, h * 0.18f, w * 0.76f, h * 0.22f)
            lineTo(w * 0.76f, h * 0.50f)
            cubicTo(w * 0.76f, h * 0.68f, w * 0.54f, h * 0.80f, w * 0.50f, h * 0.86f)
            cubicTo(w * 0.46f, h * 0.80f, w * 0.24f, h * 0.68f, w * 0.24f, h * 0.50f)
            close()
        }
        drawPath(
            path = innerShieldPath,
            color = Color(0x2239FF14),
            style = Stroke(width = 1.dp.toPx())
        )

        // 2. Lightning Bolt Path in the Center
        val boltPath = Path().apply {
            moveTo(w * 0.56f, h * 0.24f) // Top peak right
            lineTo(w * 0.38f, h * 0.50f) // Left bend
            lineTo(w * 0.49f, h * 0.50f) // Center cut
            lineTo(w * 0.42f, h * 0.82f) // Bottom sharp tip
            lineTo(w * 0.64f, h * 0.45f) // Right bend
            lineTo(w * 0.52f, h * 0.45f) // Center cut
            close()
        }

        // Glow Layer for Lightning Bolt
        drawPath(
            path = boltPath,
            color = Color(0x6639FF14),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Solid Neon Green Fill
        val boltBrush = Brush.verticalGradient(
            colors = listOf(ElectricGreenBright, ElectricNeonGreen, Color(0xFF1CB03A))
        )
        drawPath(path = boltPath, brush = boltBrush)
    }
}

// ==========================================
// 2. FILE LOADER CARD
// ==========================================
@Composable
fun FileLoaderCard(
    loadedDump: LoadedDumpState?,
    onOpenFile: () -> Unit,
    onLoadDemo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (loadedDump != null) ElectricNeonGreen else DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Arquivo",
                        tint = ElectricGreenBright,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Arquivo EEPROM",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                // File Format Badge
                if (loadedDump != null) {
                    val badgeText = if (loadedDump.isHexTextOrigin) "HEX ASCII" else "BINÁRIO"
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0x3326D94A),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(1.dp, ElectricNeonGreen, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = ElectricGreenBright
                        )
                    }
                }
            }

            // Loaded file status details
            if (loadedDump != null) {
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Carregado",
                                tint = ElectricGreenBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = loadedDump.fileName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Tamanho: ${loadedDump.rawBytes.size} bytes (${String.format("0x%04X", loadedDump.rawBytes.size)}) | Mínimo 0x0250 satisfeito",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                Text(
                    text = "Carregue um dump de bateria (.bin, .dump) ou texto ASCII com pares hexadecimais (ex: '01 00 00...').",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary File Picker Button
                OutlinedButton(
                    onClick = onOpenFile,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("load_file_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MetallicSilver),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        tint = ElectricGreenBright,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (loadedDump == null) "Carregar Arquivo" else "Substituir",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Demo Dump Button for Instant Testing
                OutlinedButton(
                    onClick = onLoadDemo,
                    modifier = Modifier.testTag("demo_dump_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0xFF141414),
                        contentColor = MetallicSilverLight
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MetallicSilver,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Demo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ==========================================
// ERROR BANNER
// ==========================================
@Composable
fun ErrorBanner(message: String) {
    Surface(
        color = Color(0x22FF5252),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ErrorRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Erro",
                tint = ErrorRed,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
            )
        }
    }
}

// ==========================================
// 3. CURRENT DUMP READOUT CARD (Dashboard Grid)
// ==========================================
@Composable
fun CurrentDumpReadoutCard(loadedDump: LoadedDumpState?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = "Leitura",
                        tint = MetallicSilver,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Leitura Atual do Dump",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (loadedDump != null) {
                    // Mirror synchronization badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val isSynced = loadedDump.isMirrorSynced
                        Icon(
                            imageVector = if (isSynced) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isSynced) ElectricGreenBright else ErrorRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isSynced) "Espelho OK" else "Espelho Diferente",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (isSynced) ElectricGreenBright else ErrorRed
                        )
                    }
                }
            }

            if (loadedDump == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nenhum arquivo carregado. Abra um dump para inspecionar os offsets.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Dashboard Stat Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Serial (SN1)
                    StatTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sn1_display"),
                        title = "Serial 1 (SN1)",
                        offsetLabel = "Offset 0x009C (14B)",
                        value = loadedDump.originalSn1.ifEmpty { "[Vazio]" },
                        subValue = "Raw: " + formatHexSlice(loadedDump.rawBytes, OFFSET_SN1, 4) + "..."
                    )

                    // Secondary Serial (SN2)
                    StatTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sn2_display"),
                        title = "Serial 2 (SN2)",
                        offsetLabel = "Offset 0x011E (15B)",
                        value = loadedDump.originalSn2.ifEmpty { "[Vazio]" },
                        subValue = "Raw: " + formatHexSlice(loadedDump.rawBytes, OFFSET_SN2, 4) + "..."
                    )
                }

                // Cycle Count Readout Row
                Surface(
                    color = DarkSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF2C2C2E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cycles_display")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Contagem de Ciclos Registrada",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${loadedDump.originalCycles}",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp
                                    ),
                                    color = ElectricGreenBright
                                )
                                Text(
                                    text = "ciclos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MetallicSilverLight,
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            Text(
                                text = "Ref (+1): ${loadedDump.originalRefCycles} | Espelho: ${loadedDump.mirrorCycles}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        // Hex Little Endian Box
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F0F0F), RoundedCornerShape(6.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Hex LE (0x01A6)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = TextMuted
                                )
                                Text(
                                    text = formatUInt16LEHex(loadedDump.originalCycles),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MetallicSilverLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatTile(
    modifier: Modifier = Modifier,
    title: String,
    offsetLabel: String,
    value: String,
    subValue: String
) {
    Surface(
        modifier = modifier,
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MetallicSilver
            )
            Text(
                text = offsetLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ==========================================
// 4. EDIT PARAMETERS FORM CARD
// ==========================================
@Composable
fun EditParametersCard(
    isEnabled: Boolean,
    sn1: String,
    onSn1Change: (String) -> Unit,
    sn2: String,
    onSn2Change: (String) -> Unit,
    cyclesText: String,
    onCyclesTextChange: (String) -> Unit,
    onResetToZero: () -> Unit,
    onIncrementCycles: (Int) -> Unit,
    onRestoreOriginal: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = "Editar",
                        tint = ElectricGreenBright,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Editar Parâmetros",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                if (isEnabled) {
                    Text(
                        text = "Restaurar",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MetallicSilver
                        ),
                        modifier = Modifier
                            .clickable { onRestoreOriginal() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // 1. Primary Serial (SN1)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Serial Primário (SN1) [Offset 0x009C]",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MetallicSilverLight
                    )
                    Text(
                        text = "${sn1.length} / $LEN_SN1",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (sn1.length == LEN_SN1) ElectricGreenBright else TextMuted
                    )
                }

                OutlinedTextField(
                    value = sn1,
                    onValueChange = onSn1Change,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sn1"),
                    enabled = isEnabled,
                    singleLine = true,
                    placeholder = { Text("Ex: VM-BAT-202401", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricNeonGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // 2. Secondary Serial (SN2)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Serial Secundário (SN2) [Offset 0x011E]",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MetallicSilverLight
                    )
                    Text(
                        text = "${sn2.length} / $LEN_SN2",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (sn2.length == LEN_SN2) ElectricGreenBright else TextMuted
                    )
                }

                OutlinedTextField(
                    value = sn2,
                    onValueChange = onSn2Change,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sn2"),
                    enabled = isEnabled,
                    singleLine = true,
                    placeholder = { Text("Ex: VM-6S-5200-PRO", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricNeonGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // 3. New Cycle Count (Numeric keyboard, 0 to 65535)
            val currentIntCycles = cyclesText.toIntOrNull()
            val isValidCycle = currentIntCycles != null && currentIntCycles in 0..65535

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Nova Contagem de Ciclos [0 a 65535]",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MetallicSilverLight
                    )
                    Text(
                        text = "Offsets 0x01A6 & 0x0226",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextMuted
                    )
                }

                OutlinedTextField(
                    value = cyclesText,
                    onValueChange = onCyclesTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cycles"),
                    enabled = isEnabled,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Ex: 0 (novo) ou 25", color = TextMuted) },
                    isError = isEnabled && !isValidCycle && cyclesText.isNotEmpty(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricNeonGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                // Live Little Endian Hex Preview
                if (currentIntCycles != null && isValidCycle) {
                    val hexLe = formatUInt16LEHex(currentIntCycles)
                    val refLe = formatUInt16LEHex((currentIntCycles + 1) and 0xFFFF)
                    Surface(
                        color = Color(0xFF0F1A12),
                        border = BorderStroke(1.dp, Color(0xFF1E4624)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$currentIntCycles ciclos -> Hex (LE): $hexLe",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = ElectricGreenBright
                            )
                            Text(
                                text = "Ref (+1): $refLe",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MetallicSilverLight
                            )
                        }
                    }
                } else if (cyclesText.isNotEmpty()) {
                    Text(
                        text = "Valor inválido. Digite um número inteiro entre 0 e 65535.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed
                    )
                }

                // Quick Action Chips
                if (isEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QuickActionChip(
                            label = "Zerar (0 ciclos)",
                            onClick = onResetToZero,
                            isHighlighted = currentIntCycles == 0
                        )
                        QuickActionChip(
                            label = "+1 ciclo",
                            onClick = { onIncrementCycles(1) }
                        )
                        QuickActionChip(
                            label = "+5 ciclos",
                            onClick = { onIncrementCycles(5) }
                        )
                        QuickActionChip(
                            label = "+10 ciclos",
                            onClick = { onIncrementCycles(10) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    onClick: () -> Unit,
    isHighlighted: Boolean = false
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isHighlighted) Color(0x3326D94A) else Color(0xFF1F1F1F),
        border = BorderStroke(1.dp, if (isHighlighted) ElectricNeonGreen else DarkBorder),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (isHighlighted) ElectricGreenBright else MetallicSilverLight,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

// ==========================================
// 5. MEMORY OFFSET INSPECTOR (Collapsible)
// ==========================================
@Composable
fun MemoryInspectorCard(
    isOpen: Boolean,
    onToggle: () -> Unit,
    dump: LoadedDumpState,
    previewSn1: String,
    previewSn2: String,
    previewCycles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MetallicSilver,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Inspecionar Offsets Hexadecimais",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Icon(
                    imageVector = if (isOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MetallicSilver
                )
            }

            AnimatedVisibility(visible = isOpen) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Comparação dos blocos de memória antes e depois das edições:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    // 1. SN1 Offset
                    MemoryOffsetRow(
                        title = "SN1 (Offset 0x009C, 14 bytes)",
                        originalHex = formatHexSlice(dump.rawBytes, OFFSET_SN1, LEN_SN1),
                        modifiedHex = run {
                            val temp = ByteArray(LEN_SN1)
                            writeAsciiString(temp, 0, LEN_SN1, previewSn1)
                            temp.joinToString(" ") { String.format("%02X", it) }
                        }
                    )

                    // 2. SN2 Offset
                    MemoryOffsetRow(
                        title = "SN2 (Offset 0x011E, 15 bytes)",
                        originalHex = formatHexSlice(dump.rawBytes, OFFSET_SN2, LEN_SN2),
                        modifiedHex = run {
                            val temp = ByteArray(LEN_SN2)
                            writeAsciiString(temp, 0, LEN_SN2, previewSn2)
                            temp.joinToString(" ") { String.format("%02X", it) }
                        }
                    )

                    // 3. Primary Block Cycles & Ref (0x01A6 - 4 bytes)
                    MemoryOffsetRow(
                        title = "Bloco Primário (0x01A6 Cycles + 0x01A8 Ref)",
                        originalHex = formatHexSlice(dump.rawBytes, OFFSET_PRIMARY_CYCLES, 4),
                        modifiedHex = run {
                            val cLe = formatUInt16LEHex(previewCycles)
                            val rLe = formatUInt16LEHex((previewCycles + 1) and 0xFFFF)
                            "$cLe $rLe"
                        }
                    )

                    // 4. Mirror Block Cycles & Ref (0x0226 - 4 bytes)
                    MemoryOffsetRow(
                        title = "Bloco Espelho (0x0226 Cycles + 0x0228 Ref)",
                        originalHex = formatHexSlice(dump.rawBytes, OFFSET_MIRROR_CYCLES, 4),
                        modifiedHex = run {
                            val cLe = formatUInt16LEHex(previewCycles)
                            val rLe = formatUInt16LEHex((previewCycles + 1) and 0xFFFF)
                            "$cLe $rLe"
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryOffsetRow(
    title: String,
    originalHex: String,
    modifiedHex: String
) {
    Surface(
        color = DarkSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Color(0xFF262626)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MetallicSilverLight
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Original:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = TextMuted)
                    Text(
                        text = originalHex,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFAAAAAA)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Novo:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = ElectricGreenBright)
                    Text(
                        text = modifiedHex,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = ElectricGreenBright
                    )
                }
            }
        }
    }
}

// ==========================================
// 6. EXPORT CARD & BUTTON
// ==========================================
@Composable
fun ExportSection(
    isEnabled: Boolean,
    exportAsBinary: Boolean,
    onFormatToggle: (Boolean) -> Unit,
    onExport: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Export Format Selector Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Formato de Exportação:",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MetallicSilverLight
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormatToggleChip(
                    label = "Binário (.bin)",
                    isSelected = exportAsBinary,
                    onClick = { onFormatToggle(true) }
                )
                FormatToggleChip(
                    label = "Hex Texto (.txt)",
                    isSelected = !exportAsBinary,
                    onClick = { onFormatToggle(false) }
                )
            }
        }

        // Full-Width Bright Electric Green Action Button
        Button(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("export_button")
                .shadow(
                    elevation = if (isEnabled) 10.dp else 0.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = ElectricGreenBright
                ),
            enabled = isEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricNeonGreen,
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFF1E2E21),
                disabledContentColor = Color(0xFF6B726C)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Exportar",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "GRAVAR E EXPORTAR ARQUIVO",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun FormatToggleChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        color = if (isSelected) Color(0x3326D94A) else Color(0xFF1A1A1A),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (isSelected) ElectricNeonGreen else DarkBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) ElectricGreenBright else TextMuted,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
