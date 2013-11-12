package uol.watchitgotit.engine;

import java.util.Vector;

import com.google.zxing.WriterException;
import com.google.zxing.common.ByteArray;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.common.reedsolomon.GF256;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.decoder.Mode;
import com.google.zxing.qrcode.decoder.Version;
import com.google.zxing.qrcode.encoder.BitVector;
import com.google.zxing.qrcode.encoder.BlockPair;
import com.google.zxing.qrcode.encoder.MaskUtil;
import com.google.zxing.qrcode.encoder.MatrixUtil;
import com.google.zxing.qrcode.encoder.QRCode;

/**
 * @author satorux@google.com (Satoru Takabayashi) - creator
 * @author dswitkin@google.com (Daniel Switkin) - ported from C++
 */
public final class QrByteEncoder {

  private QrByteEncoder() {
  }

  // The mask penalty calculation is complicated.  See Table 21 of JISX0510:2004 (p.45) for details.
  // Basically it applies four rules and summate all penalties.
  private static int calculateMaskPenalty(ByteMatrix matrix) {
    int penalty = 0;
    penalty += MaskUtil.applyMaskPenaltyRule1(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule2(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule3(matrix);
    penalty += MaskUtil.applyMaskPenaltyRule4(matrix);
    return penalty;
  }

  /**
   *  Encode "bytes" with the error correction level "ecLevel". The encoding mode will be
   *  Mode.BYTE. On success, store the result in "qrCode".
   *
   * We recommend you to use QRCode.EC_LEVEL_L (the lowest level) for
   * "getECLevel" since our primary use is to show QR code on desktop screens. We don't need very
   * strong error correction for this purpose.
   *
   */
  public static void encode(byte[] content, ErrorCorrectionLevel ecLevel, QRCode qrCode) throws WriterException {

    // Step 1: Choose the mode (encoding).
    Mode mode = Mode.BYTE;

    // Step 2: Append "bytes" into "dataBits" in appropriate encoding.
    BitVector dataBits = new BitVector();
    append8BitBytes(content, dataBits);
    // Step 3: Initialize QR code that can contain "dataBits".
    int numInputBytes = dataBits.sizeInBytes();
    initQRCode(numInputBytes, ecLevel, mode, qrCode);

    // Step 4: Build another bit vector that contains header and data.
    BitVector headerAndDataBits = new BitVector();

    // Step 4.5: Append ECI message if applicable
    // this step is skipped

    appendModeInfo(mode, headerAndDataBits);

    int numLetters = dataBits.sizeInBytes();
    appendLengthInfo(numLetters, qrCode.getVersion(), mode, headerAndDataBits);
    headerAndDataBits.appendBitVector(dataBits);

    // Step 5: Terminate the bits properly.
    terminateBits(qrCode.getNumDataBytes(), headerAndDataBits);

    // Step 6: Interleave data bits with error correction code.
    BitVector finalBits = new BitVector();
    interleaveWithECBytes(headerAndDataBits, qrCode.getNumTotalBytes(), qrCode.getNumDataBytes(),
        qrCode.getNumRSBlocks(), finalBits);

    // Step 7: Choose the mask pattern and set to "qrCode".
    ByteMatrix matrix = new ByteMatrix(qrCode.getMatrixWidth(), qrCode.getMatrixWidth());
    qrCode.setMaskPattern(chooseMaskPattern(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
        matrix));

    // Step 8.  Build the matrix and set it to "qrCode".
    MatrixUtil.buildMatrix(finalBits, qrCode.getECLevel(), qrCode.getVersion(),
        qrCode.getMaskPattern(), matrix);
    qrCode.setMatrix(matrix);
    // Step 9.  Make sure we have a valid QR Code.
    if (!qrCode.isValid()) {
      throw new WriterException("Invalid QR code: " + qrCode.toString());
    }
  }

  private static int chooseMaskPattern(BitVector bits, ErrorCorrectionLevel ecLevel, int version,
      ByteMatrix matrix) throws WriterException {

    int minPenalty = Integer.MAX_VALUE;  // Lower penalty is better.
    int bestMaskPattern = -1;
    // We try all mask patterns to choose the best one.
    for (int maskPattern = 0; maskPattern < QRCode.NUM_MASK_PATTERNS; maskPattern++) {
      MatrixUtil.buildMatrix(bits, ecLevel, version, maskPattern, matrix);
      int penalty = calculateMaskPenalty(matrix);
      if (penalty < minPenalty) {
        minPenalty = penalty;
        bestMaskPattern = maskPattern;
      }
    }
    return bestMaskPattern;
  }

  /**
   * Initialize "qrCode" according to "numInputBytes", "ecLevel", and "mode". On success,
   * modify "qrCode".
   */
  private static void initQRCode(int numInputBytes, ErrorCorrectionLevel ecLevel, Mode mode,
      QRCode qrCode) throws WriterException {
    qrCode.setECLevel(ecLevel);
    qrCode.setMode(mode);

    // In the following comments, we use numbers of Version 7-H.
    for (int versionNum = 1; versionNum <= 40; versionNum++) {
      Version version = Version.getVersionForNumber(versionNum);
      // numBytes = 196
      int numBytes = version.getTotalCodewords();
      // getNumECBytes = 130
      Version.ECBlocks ecBlocks = version.getECBlocksForLevel(ecLevel);
      int numEcBytes = ecBlocks.getTotalECCodewords();
      // getNumRSBlocks = 5
      int numRSBlocks = ecBlocks.getNumBlocks();
      // getNumDataBytes = 196 - 130 = 66
      int numDataBytes = numBytes - numEcBytes;
      // We want to choose the smallest version which can contain data of "numInputBytes" + some
      // extra bits for the header (mode info and length info). The header can be three bytes
      // (precisely 4 + 16 bits) at most. Hence we do +3 here.
      if (numDataBytes >= numInputBytes + 3) {
        // Yay, we found the proper rs block info!
        qrCode.setVersion(versionNum);
        qrCode.setNumTotalBytes(numBytes);
        qrCode.setNumDataBytes(numDataBytes);
        qrCode.setNumRSBlocks(numRSBlocks);
        // getNumECBytes = 196 - 66 = 130
        qrCode.setNumECBytes(numEcBytes);
        // matrix width = 21 + 6 * 4 = 45
        qrCode.setMatrixWidth(version.getDimensionForVersion());
        return;
      }
    }
    throw new WriterException("Cannot find proper rs block info (input data too big?)");
  }

  /**
   * Terminate bits as described in 8.4.8 and 8.4.9 of JISX0510:2004 (p.24).
   */
  static void terminateBits(int numDataBytes, BitVector bits) throws WriterException {
    int capacity = numDataBytes << 3;
    if (bits.size() > capacity) {
      throw new WriterException("data bits cannot fit in the QR Code" + bits.size() + " > " +
          capacity);
    }
    // Append termination bits. See 8.4.8 of JISX0510:2004 (p.24) for details.
    // TODO: srowen says we can remove this for loop, since the 4 terminator bits are optional if
    // the last byte has less than 4 bits left. So it amounts to padding the last byte with zeroes
    // either way.
    for (int i = 0; i < 4 && bits.size() < capacity; ++i) {
      bits.appendBit(0);
    }
    int numBitsInLastByte = bits.size() % 8;
    // If the last byte isn't 8-bit aligned, we'll add padding bits.
    if (numBitsInLastByte > 0) {
      int numPaddingBits = 8 - numBitsInLastByte;
      for (int i = 0; i < numPaddingBits; ++i) {
        bits.appendBit(0);
      }
    }
    // Should be 8-bit aligned here.
    if (bits.size() % 8 != 0) {
      throw new WriterException("Number of bits is not a multiple of 8");
    }
    // If we have more space, we'll fill the space with padding patterns defined in 8.4.9 (p.24).
    int numPaddingBytes = numDataBytes - bits.sizeInBytes();
    for (int i = 0; i < numPaddingBytes; ++i) {
      if (i % 2 == 0) {
        bits.appendBits(0xec, 8);
      } else {
        bits.appendBits(0x11, 8);
      }
    }
    if (bits.size() != capacity) {
      throw new WriterException("Bits size does not equal capacity");
    }
  }

  /**
   * Get number of data bytes and number of error correction bytes for block id "blockID". Store
   * the result in "numDataBytesInBlock", and "numECBytesInBlock". See table 12 in 8.5.1 of
   * JISX0510:2004 (p.30)
   */
  static void getNumDataBytesAndNumECBytesForBlockID(int numTotalBytes, int numDataBytes,
      int numRSBlocks, int blockID, int[] numDataBytesInBlock,
      int[] numECBytesInBlock) throws WriterException {
    if (blockID >= numRSBlocks) {
      throw new WriterException("Block ID too large");
    }
    // numRsBlocksInGroup2 = 196 % 5 = 1
    int numRsBlocksInGroup2 = numTotalBytes % numRSBlocks;
    // numRsBlocksInGroup1 = 5 - 1 = 4
    int numRsBlocksInGroup1 = numRSBlocks - numRsBlocksInGroup2;
    // numTotalBytesInGroup1 = 196 / 5 = 39
    int numTotalBytesInGroup1 = numTotalBytes / numRSBlocks;
    // numTotalBytesInGroup2 = 39 + 1 = 40
    int numTotalBytesInGroup2 = numTotalBytesInGroup1 + 1;
    // numDataBytesInGroup1 = 66 / 5 = 13
    int numDataBytesInGroup1 = numDataBytes / numRSBlocks;
    // numDataBytesInGroup2 = 13 + 1 = 14
    int numDataBytesInGroup2 = numDataBytesInGroup1 + 1;
    // numEcBytesInGroup1 = 39 - 13 = 26
    int numEcBytesInGroup1 = numTotalBytesInGroup1 - numDataBytesInGroup1;
    // numEcBytesInGroup2 = 40 - 14 = 26
    int numEcBytesInGroup2 = numTotalBytesInGroup2 - numDataBytesInGroup2;
    // Sanity checks.
    // 26 = 26
    if (numEcBytesInGroup1 != numEcBytesInGroup2) {
      throw new WriterException("EC bytes mismatch");
    }
    // 5 = 4 + 1.
    if (numRSBlocks != numRsBlocksInGroup1 + numRsBlocksInGroup2) {
      throw new WriterException("RS blocks mismatch");
    }
    // 196 = (13 + 26) * 4 + (14 + 26) * 1
    if (numTotalBytes !=
        ((numDataBytesInGroup1 + numEcBytesInGroup1) *
            numRsBlocksInGroup1) +
            ((numDataBytesInGroup2 + numEcBytesInGroup2) *
                numRsBlocksInGroup2)) {
      throw new WriterException("Total bytes mismatch");
    }

    if (blockID < numRsBlocksInGroup1) {
      numDataBytesInBlock[0] = numDataBytesInGroup1;
      numECBytesInBlock[0] = numEcBytesInGroup1;
    } else {
      numDataBytesInBlock[0] = numDataBytesInGroup2;
      numECBytesInBlock[0] = numEcBytesInGroup2;
    }
  }

  /**
   * Interleave "bits" with corresponding error correction bytes. On success, store the result in
   * "result". The interleave rule is complicated. See 8.6 of JISX0510:2004 (p.37) for details.
   */
  static void interleaveWithECBytes(BitVector bits, int numTotalBytes,
      int numDataBytes, int numRSBlocks, BitVector result) throws WriterException {

    // "bits" must have "getNumDataBytes" bytes of data.
    if (bits.sizeInBytes() != numDataBytes) {
      throw new WriterException("Number of bits and data bytes does not match");
    }

    // Step 1.  Divide data bytes into blocks and generate error correction bytes for them. We'll
    // store the divided data bytes blocks and error correction bytes blocks into "blocks".
    int dataBytesOffset = 0;
    int maxNumDataBytes = 0;
    int maxNumEcBytes = 0;

    // Since, we know the number of reedsolmon blocks, we can initialize the vector with the number.
    Vector blocks = new Vector(numRSBlocks);

    for (int i = 0; i < numRSBlocks; ++i) {
      int[] numDataBytesInBlock = new int[1];
      int[] numEcBytesInBlock = new int[1];
      getNumDataBytesAndNumECBytesForBlockID(
          numTotalBytes, numDataBytes, numRSBlocks, i,
          numDataBytesInBlock, numEcBytesInBlock);

      ByteArray dataBytes = new ByteArray();
      dataBytes.set(bits.getArray(), dataBytesOffset, numDataBytesInBlock[0]);
      ByteArray ecBytes = generateECBytes(dataBytes, numEcBytesInBlock[0]);
      blocks.addElement(new BlockPair(dataBytes, ecBytes));

      maxNumDataBytes = Math.max(maxNumDataBytes, dataBytes.size());
      maxNumEcBytes = Math.max(maxNumEcBytes, ecBytes.size());
      dataBytesOffset += numDataBytesInBlock[0];
    }
    if (numDataBytes != dataBytesOffset) {
      throw new WriterException("Data bytes does not match offset");
    }

    // First, place data blocks.
    for (int i = 0; i < maxNumDataBytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        ByteArray dataBytes = ((BlockPair) blocks.elementAt(j)).getDataBytes();
        if (i < dataBytes.size()) {
          result.appendBits(dataBytes.at(i), 8);
        }
      }
    }
    // Then, place error correction blocks.
    for (int i = 0; i < maxNumEcBytes; ++i) {
      for (int j = 0; j < blocks.size(); ++j) {
        ByteArray ecBytes = ((BlockPair) blocks.elementAt(j)).getErrorCorrectionBytes();
        if (i < ecBytes.size()) {
          result.appendBits(ecBytes.at(i), 8);
        }
      }
    }
    if (numTotalBytes != result.sizeInBytes()) {  // Should be same.
      throw new WriterException("Interleaving error: " + numTotalBytes + " and " +
          result.sizeInBytes() + " differ.");
    }
  }

  static ByteArray generateECBytes(ByteArray dataBytes, int numEcBytesInBlock) {
    int numDataBytes = dataBytes.size();
    int[] toEncode = new int[numDataBytes + numEcBytesInBlock];
    for (int i = 0; i < numDataBytes; i++) {
      toEncode[i] = dataBytes.at(i);
    }
    new ReedSolomonEncoder(GF256.QR_CODE_FIELD).encode(toEncode, numEcBytesInBlock);

    ByteArray ecBytes = new ByteArray(numEcBytesInBlock);
    for (int i = 0; i < numEcBytesInBlock; i++) {
      ecBytes.set(i, toEncode[numDataBytes + i]);
    }
    return ecBytes;
  }

  /**
   * Append mode info. On success, store the result in "bits".
   */
  static void appendModeInfo(Mode mode, BitVector bits) {
    bits.appendBits(mode.getBits(), 4);
  }


  /**
   * Append length info. On success, store the result in "bits".
   */
  static void appendLengthInfo(int numLetters, int version, Mode mode, BitVector bits)
      throws WriterException {
    int numBits = mode.getCharacterCountBits(Version.getVersionForNumber(version));
    if (numLetters > ((1 << numBits) - 1)) {
      throw new WriterException(numLetters + "is bigger than" + ((1 << numBits) - 1));
    }
    bits.appendBits(numLetters, numBits);
  }

  static void append8BitBytes(byte[] content, BitVector bits)
      throws WriterException {
    for (int i = 0; i < content.length; ++i) {
      bits.appendBits(content[i], 8);
    }
  }

}
