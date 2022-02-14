package com.z299studio.pbfree.data

import org.junit.Test
import java.util.*

import org.junit.Assert.*
import java.lang.IllegalArgumentException

class DataProcessorTest {
    private val expectedCategories = listOf(Category("Bank Accounts", 0),
        Category("Credit Cards", 1), Category("Devices", 2),
        Category("E-Banking", 3), Category("Email Accounts", 4),
        Category("Web Accounts", 5))
    private val expectedEntries = arrayOf(
        Entry("Google", "Web Accounts", arrayListOf(
            ValueTuple("Web address", "http://www.google.com", ValueType.Url),
            ValueTuple("Email", "your_account@gmail.com", ValueType.Email),
            ValueTuple("Password", "1234abcd*()", ValueType.Password))
        ),
        Entry("Facebook", "Web Accounts", arrayListOf(
            ValueTuple("Web address", "https://www.facebook.com", ValueType.Url),
            ValueTuple("Email", "your_account@gmail.com", ValueType.Email),
            ValueTuple("Password", "1234abcd*()", ValueType.Password))
        )
    )
    @Test
    fun testV1Parser() {
        val pbData = Base64.getDecoder().decode("UEICECAQICAgICAgICAgMTUXBeOqGnMe89W8ip3ly3CI" +
                "OWOymdV3PeOycaHGMUAXQs4lzSaiY3hnX+Bu/Hj1HllGizaQ/EOGiRcX4NDOBNjgqSBvltTPyI4bXJRC" +
                "s5qQ2GKztiRRRiN5hUT2JA/pfwhkSAbmvz+pYLOD0JJ1lg2U83cGfrZyMUZH4uRLuCw4AiU42K05S54i" +
                "7ZLfRhfR3z27q3tq8nbD3ZuHq9Mzsj/U865+zZMJ0sQb5hdGo5waeazx/6UdxZe3AzfCKYvBt23ycFpL" +
                "KUM8xyLZqVmZPXsoN9wecXfK3ugF8YPmtslUi44nezQLwHTm7fkyksJObw4fyhRZjPi/sXSAEbjruYQ2" +
                "hRxcJOU7O3SdkavjqjCu7iy1QYhJr7k7uhCBOqc6t+UPRQNs1N9KwmAw2fVo7ajfeo4k8Rab11U+mB0t" +
                "prNMKdJ1YHvgMbYPsfNY4CbmhUxMZ2vPZWVFJQSXzQHAeIE=")
        val result = DataProcessor.getDataParser(pbData).parse("123456", pbData)
        assertTrue(expectedCategories.containsAll(result.second))
        assertArrayEquals(expectedEntries, result.first.toTypedArray())
    }

    @Test
    fun testV2ParserWithoutCategories() {
        val pbV2Data = ("""{"entries":"W3aL54N1KOYaWP64n3zJsTCa3Sf6OBM49+/BW8CwBXYr4SUi5h2/kviKt6zgX""" +
                """g27lT65e9ijUDimAd23N3hzdloiAhfItZGaC9xdGYA6/MXRCi8ddeIjR/hF3iIbDtiloIL51QSn8vpC+L+5Utv""" +
                """rOybb0aGRvJL5piuvLvpa7I955HQx2zUUFhShUY6/HFnCa2aOY5u4NoT32aVeRpID3AQimCPa+E4kK51KFwhyv""" +
                """KYXShX7v6VSkflYZaZAAnE20O+gAtbjDJE86KjSwz/7cZx8sQLuTycnVVYseiebK58c+7vshaju2Nn7Ios/+Pq""" +
                """Ab8kb26KgsX59Xyl7B+llZ3aN3hSzodgl5VbtjVNtTyBIXgbLTCIyIM2N+33mQyc15J+c10n+kF036796iJs7M""" +
                """zTi3PXkP6TBZLzSDtYKv7M1lbLdDZ1J0IrsoTXghVavtLeb8dJkW+f8NteIQH4t9vLEJyiG7x/rrb0CNs97iKT""" +
                """T+aJtHSYLCg69CAcoux+NyRvir9DLFfvb8TBmChnrM2vt3H4/RM29dFw8hnYxtnueaOvorj6nEDiDpihNfTwQh""" +
                """VzAHyB5y+PfhKkOhDxEcj7tN9Ge6+5rSwplS7gdmoSNqDFNg/JpT5fscdohU+uUYSHyQrFS9LwK2pe5yrPI1Q\""" +
                """u003d\u003d","crypto":{"method":"aes","format":"json","params":{"salt":"5174d4fb16d743""" +
                """0e48f834408ffb8a2b181e578863883fcb4c83ea8a0e6ae1bb","keySize":256,"iteration":2000,"iv""" +
                """":"b961bceb846becc797fd5df856682744"}},"app":"test","version":"1"}""").toByteArray()
        val result = DataProcessor.getDataParser(pbV2Data).parse("123456", pbV2Data)
        //assertTrue(result.second.isEmpty())
        assertArrayEquals(expectedEntries, result.first.toTypedArray())
    }

    @Test
    fun testV2ParserWithCategories() {
        val pbV2Data = ("""{"app":"test","version":"1.0","saveTime":0,"dataVersion":0,"crypto":{"method"""" +
                """:"aes","params":{"keySize":256,"iteration":2000,"iv":"585666dff325930ae09bd5147576698d","sa""" +
                """lt":"f54d60555df3bae4acd0d7e8a80799ae72aecad177944ce66ea008a138026b44"}},"entries":"YAtk2tH""" +
                """oE5MIKDqjrFJi/IP/fa8eQ0zMp5xPVrtqHF7MN90lc08NsjAjMtG+Nmm32qfXHlUy1biL8a/VOiUj733OsBHtDpSqbV""" +
                """cfhevgmdQam/E+nAbLQWcy731ogBNWbsD32PXRybt/rlSSeWXTuMkCLrAwDTKyrpn+zNlk6f5S5AvjLFgf721tmClKA""" +
                """3zBLQ9IyVlfFG2VIGhJoVPouiKo6aWXkz4bkD18p8a3wCoVBPCh+afbaVLxsMFpg0oEs/jIfYCBVBJDE/5vUmnYYYcB""" +
                """Hbb+CRr+iqK5XyA2uOlVphfSP9zjDuQGjnarX2EBfS1d3VJ6u+lamjHY1DYYhBRZ903AiPhKEs7HF0ydPOREeufj23G""" +
                """U1i+/Z4KbWv/f6ru2k2Todr6yq85RRCcmuv6cswIcBSYA7Dg5TghThx6R1SaKNp5oxKRJzBTvBzivmsACU061humZ4c""" +
                """mNYURtLMLTOMT+YCZQwKsGZ3YGJAxZu/PS3YdRBb/VkqNmmZgbZZ7PS3gzg6Mg7JXDXqnlbm89ziz2mfIIU1hPHsl4P""" +
                """iIoAzy+fSmRy+R9x9xFQSK9IZhdNxGdJa/ppXoS3/CAvw4L5QwmSQuwlGc2eKkgFl8sQ4zvdL4cEenzPx+UsPc7/jPj""" +
                """RnWjL638q2VYR3ru8gRngLANMiMik7dmHGOi5UuI7cC6VBEvlC2DObXqIyDTDOz00lVJouvrUiwqVD1qNU5ZAUvcSWI""" +
                """ECsQqg4OvV2JrACjiwA47HYUMmxCfKXu+S6QMMSdpxy5kaQVRRTAahQK5r0EyioHOIb+Tu2FpTtx//Ggmgt0BhynAMR""" +
                """AKF9gHJJxvamr7glGPaoEDT07oDZTjxcZf7py+gVmPiJxMYHOYnUnZoFRAQe83m63HEDZlEHzAArU5fJdDSuojQz3Rz""" +
                """rWYpUMXbbK5hAJ9/QjI6FsoaLXqEZh/cvXxq89N2wB6"}""").toByteArray()
        val result = DataProcessor.getDataParser(pbV2Data).parse("123456", pbV2Data)
        assertTrue(expectedCategories.containsAll(result.second))
        assertArrayEquals(expectedEntries, result.first.toTypedArray())
    }

    @Test
    fun testV2Converter() {
        val converter = DataProcessor.getConverter()
        val result = converter.toByteArray(AppInfo("test", "1.0"),
        "123456",expectedCategories,  expectedEntries.toList())
        assertTrue(result.isNotEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNoParser() {
        val pbData = Base64.getDecoder()
            .decode("OWOymdV3PeOycaHGMUAXQs4lzSaiY3hnX+Bu/Hj1HllGizaQ/EOGiRcX4NDOBNjgqSB")
        DataProcessor.getDataParser(pbData)
    }

}