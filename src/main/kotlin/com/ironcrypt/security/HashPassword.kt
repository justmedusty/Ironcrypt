package com.ironcrypt.security

import org.mindrot.jbcrypt.BCrypt

fun hashPassword(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt())
}