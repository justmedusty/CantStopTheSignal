package cantstopthesignal.database.admin


/*
    I do not really want deletions to be much of a thing because it can encourage the very thing that this service is meant to combat, but
    there does need to be some way to handle spam or well poisoning. Suspending + making a service invite only is probably a good way to do it.
    I will decide if I want to implement admin removal at all because in my opinion if something is so bad that it warrants removal the person posting
    it is probably not welcome in the forum anyway.

    I'll let this stew for a bit before making a decision. I am leaning toward making "deleted" a boolean flag to not kill comments or replies that may have been useful to others.
    I will likely just implement soft deletion and maybe allow admins to soft delete, if I do, it will have a clear message saying this was removed by admins for X reason for transparency
 */