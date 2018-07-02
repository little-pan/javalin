/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.ApiBuilder.get
import io.javalin.TestAccessManager.MyRoles.ROLE_ONE
import io.javalin.TestAccessManager.MyRoles.ROLE_TWO
import io.javalin.security.AccessManager
import io.javalin.security.Role
import io.javalin.security.SecurityUtil.roles
import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestAccessManager : BaseTest() {

    enum class MyRoles : Role { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private val accessManager = AccessManager { handler, ctx, permittedRoles ->
        val userRole = ctx.queryParam("role")
        if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
            handler.handle(ctx)
        } else {
            ctx.status(401).result("Unauthorized")
        }
    }

    @Test
    fun test_noopAccessManager_throwsException_whenRoles() {
        app.get("/secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE))
        assertThat(callWithRole("/secured", "ROLE_ONE"), `is`("Internal server error"))
    }

    @Test
    fun test_accessManager_restrictsAccess() {
        app.accessManager(accessManager)
        app.get("/secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE, ROLE_TWO))
        assertThat(callWithRole("/secured", "ROLE_ONE"), `is`("Hello"))
        assertThat(callWithRole("/secured", "ROLE_TWO"), `is`("Hello"))
        assertThat(callWithRole("/secured", "ROLE_THREE"), `is`("Unauthorized"))
    }

    @Test
    fun test_accessManager_restrictsAccess_forStaticApi() {
        app.accessManager(accessManager)
        app.routes { get("/static-secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE, ROLE_TWO)) }
        assertThat(callWithRole("/static-secured", "ROLE_ONE"), `is`("Hello"))
        assertThat(callWithRole("/static-secured", "ROLE_TWO"), `is`("Hello"))
        assertThat(callWithRole("/static-secured", "ROLE_THREE"), `is`("Unauthorized"))
    }

    private fun callWithRole(path: String, role: String) = Unirest.get(origin + path).queryString("role", role).asString().body

}
