package cantstopthesignal.routing.comments

import cantstopthesignal.database.comments.dislikeComment
import cantstopthesignal.database.comments.likeComment
import cantstopthesignal.database.comments.verifyCommentId
import cantstopthesignal.database.posts.verifyPostId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureCommentLikesRouting() {
    routing {
        authenticate("jwt") {

            //The database layer will handle unliking or undisliking etc
            post("/comments/{postId}//like/{commentId}") {
                val commentId =
                    call.parameters["commentId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val postId =
                    call.parameters["postId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest
                )

                val validPostId = verifyPostId(postId)
                if (!validPostId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val validCommentId = verifyCommentId(commentId)
                if (!validCommentId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val success = likeComment(userId, commentId)
                val successMessage = "Comment liked"
                val redirect = call.request.queryParameters["redirect"] ?: "/comments/$postId/replies/$commentId"
                val separator = if (redirect.contains("?")) "&" else "?"
                if (!success) {
                    val errorMessage = "An error occurred"
                    return@post call.respondRedirect(redirect + separator + "error=$errorMessage")
                }
                return@post call.respondRedirect(redirect + separator + "success=$successMessage")


            }
            post("/comments/{postId}/dislike/{commentId}") {
                val commentId =
                    call.parameters["commentId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val postId =
                    call.parameters["postId"]?.toLongOrNull() ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<JWTPrincipal>()?.subject?.toLongOrNull() ?: return@post call.respond(
                    HttpStatusCode.BadRequest
                )

                val validPostId = verifyPostId(postId)

                if (!validPostId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val validCommentId = verifyCommentId(commentId)

                if (!validCommentId) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                val success = dislikeComment(userId, commentId)
                val successMessage = "Comment disliked"
                /*
                    Let it be duly noted that:
                    When liking or disliking and the undo of each operation on a single page, it will keep adding the success or error message to the URL, it is ugly but it only compounds until
                    the user clicks off that particular page. The pages are not that massive, so I do not care to write finicky stripping logic to look and strip error or success if theyre there.
                    It will just piggy back off the last one if they are liking many from the same page.
                 */
                val redirect = call.request.queryParameters["redirect"] ?: "/comments/$postId/replies/$commentId"
                val separator = if (redirect.contains("?")) "&" else "?"
                if (!success) {
                    val errorMessage = "An error occurred"
                    return@post call.respondRedirect(redirect + separator + "error=$errorMessage")
                }
                return@post call.respondRedirect(redirect + separator + "success=$successMessage")

            }
        }
    }
}