package com.freedom.cantstopthesignal.helper

import cantstopthesignal.log.logger
import com.freedom.cantstopthesignal.database.dsl.table_definitions.Users
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import kotlin.system.exitProcess

/**
 * Verify credentials
 *
 * @param userName
 * @param password
 * @return
 */
fun verifyCredentials(userName: String, password: String): Boolean {
    logger.debug { "Verifying credentials..." }
    return try {
        transaction {
            val user = Users.selectAll().where(Users.userName eq userName ).singleOrNull()
            if(user == null) {
                logger.error { "User $userName does not exist." }
            }
            val userpassword = user?.get(Users.passwordHash).toString()
            user != null && BCrypt.checkpw(password, userpassword)
        }
    } catch (e: Exception) {
        logger.error { "Error verifying credentials $e" }
        return false
    }
}