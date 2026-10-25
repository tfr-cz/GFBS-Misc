const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const palette = {
    '.': [0, 0, 0, 0],
    'K': [0x1E, 0x22, 0x26, 0xFF],
    's': [0x8E, 0x97, 0x9F, 0xFF],
    'g': [0xE8, 0xB0, 0x4B, 0xFF],
    'G': [0xB9, 0x8A, 0x45, 0xFF],
    'R': [0xE2, 0x4B, 0x36, 0xFF],
    'r': [0xFF, 0x8A, 0x6E, 0xFF],
};

const rows = [
    '................',
    '.KKKKKKK........',
    '.KsssssK........',
    '.KssKssK...RR...',
    '.KssKssK...rr...',
    '.KssKssK...GG...',
    '.KssKssK...GG...',
    '.KssKssK...GG...',
    '.KssKgsK...GG...',
    '.KssKssK...GG...',
    '.KssKssK..ssss..',
    '.KssKssK........',
    '.KsssssK........',
    '.KKKKKKK........',
    '................',
    '................',
];

const width = 16;
const height = 16;
if (rows.length !== height) throw new Error('bad row count');
for (const row of rows) if (row.length !== width) throw new Error('bad row width: ' + row);

const raw = Buffer.alloc((width * 4 + 1) * height);
for (let y = 0; y < height; y++) {
    const lineOffset = y * (width * 4 + 1);
    raw[lineOffset] = 0;
    for (let x = 0; x < width; x++) {
        const color = palette[rows[y][x]];
        if (!color) throw new Error('unknown palette char: ' + rows[y][x]);
        const offset = lineOffset + 1 + x * 4;
        raw[offset] = color[0];
        raw[offset + 1] = color[1];
        raw[offset + 2] = color[2];
        raw[offset + 3] = color[3];
    }
}

const crcTable = new Int32Array(256);
for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    crcTable[n] = c;
}

function crc32(buffer) {
    let c = 0xFFFFFFFF;
    for (const byte of buffer) c = crcTable[(c ^ byte) & 0xFF] ^ (c >>> 8);
    return (c ^ 0xFFFFFFFF) >>> 0;
}

function chunk(type, data) {
    const length = Buffer.alloc(4);
    length.writeUInt32BE(data.length, 0);
    const typeAndData = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(typeAndData), 0);
    return Buffer.concat([length, typeAndData, crc]);
}

const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(width, 0);
ihdr.writeUInt32BE(height, 4);
ihdr[8] = 8;
ihdr[9] = 6;
ihdr[10] = 0;
ihdr[11] = 0;
ihdr[12] = 0;

const png = Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
    chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
]);

const out = path.join(__dirname, '..', 'src', 'main', 'resources', 'assets', 'gfbs_doors',
    'textures', 'item', 'door_configurator.png');
fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, png);
console.log('written', out, png.length, 'bytes');
