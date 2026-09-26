package com.senda.lecturabiblica.domain

import com.senda.lecturabiblica.model.Reading
import org.junit.Assert.assertTrue
import org.junit.Test

class BibleLinksTest {
    @Test fun personalizedYouVersionLeavesTranslationUnforced() {
        val url = youVersionUrl(Reading("JHN", 3), "CUSTOM")
        assertTrue(url.startsWith("youversion://bible?reference=JHN.3.1"))
        assertTrue(!url.contains("/146/"))
    }

    @Test fun gatewayEncodesReferenceAndVersion() {
        val url = bibleGatewayUrl(Reading("GEN", 1), "NVI")
        assertTrue(url.contains("version=NVI"))
        assertTrue(url.contains("G%C3%A9nesis+1"))
    }
}
