package com.freedom.cantstopthesignal.helper

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

/**
 * Verify credentials
 *
 * @param userName
 * @param password
 * @return
 */
fun verifyCredentials(userName: String, password: String): Boolean {
    return try {
        transaction {
            val user = Users.select(Users.userName eq userName ).singleOrNull()
            val userpassword = user?.get(Users.passwordHash).toString()
            user != null && BCrypt.checkpw(password, userpassword)
        }
    } catch (e: Exception) {
        logger.error { "Error verifying credentials $e" }
        return false
    }
}