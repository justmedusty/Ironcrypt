package com.ironcrypt.enums

enum class Maximums (val value : Long) {
    MAX_FILE_SIZE_BYTES(1_000_000_000),
    MAX_USER_SPACE_BYTES(5_000_000_000),
    MAX_PUBLIC_KEY_SIZE_BYTES(1_000_000),
}

enum class Pathing(val value : String){
    USER_FILE_DIRECTORY(System.getenv("FILEPATH"))
}