package com.ironcrypt.enums

enum class Maximums (val value : Int) {
    MAX_FILE_SIZE_BYTES(1_000_000_000),
    MAX_USER_SPACE_BYTES(2_000_000_000),
    MAX_PUBLIC_KEY_SIZE_BYTES(1_000_000),
    MAX_FILE_NAME_CHAR_LENGTH(255)
}