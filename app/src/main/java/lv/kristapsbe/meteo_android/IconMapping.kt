package lv.kristapsbe.meteo_android


class IconMapping {
    companion object {
        val iconMapping: HashMap<Int, Int> = hashMapOf(
            1101 to R.drawable.clear,
            1102 to R.drawable.pcloudy,
            1103 to R.drawable.mcloudy1, // single fulled out cloud with sun
            1104 to R.drawable.mcloudy, // single filled out cloud
            1105 to R.drawable.mcloudy, // two filled out clouds

            1201 to R.drawable.sleet, // two filled out cloud with sleet
            1202 to R.drawable.sleet, // single filled out cloud with sleet
            1203 to R.drawable.sleet1, // single filled out cloud with sun and sleet
            1204 to R.drawable.sleet, // two filled out cloud with sleet
            1205 to R.drawable.sleet, // single filled out cloud with sleet
            1206 to R.drawable.sleet1, // single filled out cloud with sun and sleet
            1207 to R.drawable.sleet, // single filled out cloud with sleet
            1208 to R.drawable.sleet1, // single filled out cloud with sun and sleet

            1301 to R.drawable.tshower, // two filled out clouds, with lightning and rain
            1302 to R.drawable.tshower, // single filled out cloud, with lightning and rain
            1303 to R.drawable.tshower1, // single filled out cloud with sun, lightning and rain
            1304 to R.drawable.tshower, // two filled out clouds, with lightning and rain (downpour)
            1305 to R.drawable.tshower, // single filled out cloud, with lightning and rain (downpour)
            1306 to R.drawable.tshower, // single filled out cloud with sun, lightning and rain (downpour)
            1307 to R.drawable.tshower, // two filled out clouds, with lightning and snow or sleet
            1308 to R.drawable.tshower, // single filled out cloud, with lightning and snow or sleet
            1309 to R.drawable.tshower1, // single filled out cloud with sun, lightning and snow or sleet

            1401 to R.drawable.fog, // two clouds with fog
            1402 to R.drawable.fog, // cloud with fog
            1403 to R.drawable.fog1, // single filled out cloud with fog
            1404 to R.drawable.fog1, // single unfilled cloud with fog
            1405 to R.drawable.fog, // two clouds with fog and rain (drizzle)
            1406 to R.drawable.fog, // cloud with fog and rain (drizzle)
            1407 to R.drawable.fog, // two clouds with fog and rain
            1408 to R.drawable.fog1, // two clouds with fog, sun and rain
            1409 to R.drawable.fog, // cloud with fog and snow or sleet

            1501 to R.drawable.shower, // two clouds and drizzle
            1502 to R.drawable.shower, // cloud and drizzle
            1503 to R.drawable.shower1, // cloud, sun and drizzle
            1504 to R.drawable.rain, // two clouds and rain
            1505 to R.drawable.rain, // cloud and rain
            1506 to R.drawable.rain1, // cloud, sun and rain
            1507 to R.drawable.rain, // two clouds and rain (downpour)
            1508 to R.drawable.rain, // cloud and rain (downpour)
            1509 to R.drawable.rain1, // cloud, sun and rain (downpour)

            1601 to R.drawable.lsnow, // two clouds and snow
            1602 to R.drawable.lsnow, // cloud and snow
            1603 to R.drawable.lsnow1, // cloud, sun and snow
            1604 to R.drawable.snow, // two clouds and more snow
            1605 to R.drawable.snow, // cloud and more snow
            1606 to R.drawable.snow1, // cloud, sun and more snow
            1607 to R.drawable.snow, // two clouds, snow and wind
            1608 to R.drawable.snow1, // cloud, sun, snow and wind
            1609 to R.drawable.snow, // cloud, sun, more snow and wind

            1610 to R.drawable.sleet1, // cloud, sun, sleet and wind
            1611 to R.drawable.snow, // two clouds, snow and wind
            1612 to R.drawable.snow, // cloud, snow and wind
            1613 to R.drawable.snow1, // cloud, sun, snow and wind
            1614 to R.drawable.sleet, // two clouds, and sleet
            1615 to R.drawable.sleet, // cloud, and sleet
            1616 to R.drawable.sleet1, // cloud, sun, and sleet
            1617 to R.drawable.sleet, // two clouds, and sleet
            1618 to R.drawable.sleet, // cloud, and sleet
            1619 to R.drawable.sleet1, // cloud, sun, and sleet

            2101 to R.drawable.clear0,
            2102 to R.drawable.pcloudy0,
            2103 to R.drawable.mcloudy0,
            2104 to R.drawable.mcloudy,
            2105 to R.drawable.mcloudy,

            2201 to R.drawable.sleet, // two filled out cloud with sleet
            2202 to R.drawable.sleet, // single filled out cloud with sleet
            2203 to R.drawable.sleet0, // single filled out cloud with moon and sleet
            2204 to R.drawable.sleet, // two filled out cloud with sleet
            2205 to R.drawable.sleet, // single filled out cloud with sleet
            2206 to R.drawable.sleet0, // single filled out cloud with moon and sleet
            2207 to R.drawable.sleet, // single filled out cloud with sleet
            2208 to R.drawable.sleet0, // single filled out cloud with moon and sleet

            2301 to R.drawable.tshower, // two filled out clouds, with lightning and rain
            2302 to R.drawable.tshower, // single filled out cloud, with lightning and rain
            2303 to R.drawable.tshower0, // single filled out cloud with moon, lightning and rain
            2304 to R.drawable.tshower, // two filled out clouds, with lightning and rain (downpour)
            2305 to R.drawable.tshower, // single filled out cloud, with lightning and rain (downpour)
            2306 to R.drawable.tshower0, // single filled out cloud with moon, lightning and rain (downpour)
            2307 to R.drawable.tshower, // two filled out clouds, with lightning and snow or sleet
            2308 to R.drawable.tshower, // single filled out cloud, with lightning and snow or sleet
            2309 to R.drawable.tshower0, // single filled out cloud with moon, and lightning

            2401 to R.drawable.fog, // two clouds with fog
            2402 to R.drawable.fog, // cloud with fog
            2403 to R.drawable.fog0, // single filled out cloud with fog
            2404 to R.drawable.fog0, // single unfilled cloud with fog
            2405 to R.drawable.fog, // two clouds with fog and rain (drizzle)
            2406 to R.drawable.fog, // cloud with fog and rain (drizzle)
            2407 to R.drawable.fog, // two clouds with fog and rain
            2408 to R.drawable.fog0, // two clouds with fog, moon and rain
            2409 to R.drawable.fog, // cloud with fog and snow or sleet

            2501 to R.drawable.shower, // two clouds and drizzle
            2502 to R.drawable.shower, // cloud and drizzle
            2503 to R.drawable.shower0, // cloud, moon and drizzle
            2504 to R.drawable.rain, // two clouds and rain
            2505 to R.drawable.rain, // cloud and rain
            2506 to R.drawable.rain0, // cloud, moon and rain
            2507 to R.drawable.rain, // two clouds and rain (downpour)
            2508 to R.drawable.rain, // cloud and rain (downpour)
            2509 to R.drawable.rain0, // cloud, moon and rain (downpour)

            2601 to R.drawable.lsnow, // two clouds and snow
            2602 to R.drawable.lsnow, // cloud and snow
            2603 to R.drawable.lsnow0, // cloud, moon and snow
            2604 to R.drawable.snow, // two clouds and more snow
            2605 to R.drawable.snow, // cloud and more snow
            2606 to R.drawable.snow0, // cloud, moon and more snow
            2607 to R.drawable.snow, // two clouds, snow and wind
            2608 to R.drawable.snow, // cloud, moon, snow and wind
            2609 to R.drawable.snow0, // cloud, sun, more snow and wind

            2610 to R.drawable.sleet0, // cloud, moon, sleet and wind
            2611 to R.drawable.snow, // two clouds, snow and wind
            2612 to R.drawable.snow, // cloud, snow and wind
            2613 to R.drawable.snow0, // cloud, moon, snow and wind
            2614 to R.drawable.sleet, // two clouds, and sleet
            2615 to R.drawable.sleet, // cloud, and sleet
            2616 to R.drawable.sleet0, // cloud, moon, and sleet
            2617 to R.drawable.sleet, // two clouds, and sleet
            2618 to R.drawable.sleet, // cloud, and sleet
            2619 to R.drawable.sleet0, // cloud, moon, and sleet
        )

        val alternateIconMapping: HashMap<Int, Int> = hashMapOf(
            1101 to R.drawable.clear_day,
            1102 to R.drawable.partly_cloudy_day,
            1103 to R.drawable.overcast_day, // single fulled out cloud with sun
            1104 to R.drawable.extreme_day, // single filled out cloud
            1105 to R.drawable.extreme, // two filled out clouds

            1201 to R.drawable.extreme_sleet, // two filled out cloud with sleet
            1202 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            1203 to R.drawable.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet
            1204 to R.drawable.extreme_sleet, // two filled out cloud with sleet
            1205 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            1206 to R.drawable.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet
            1207 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            1208 to R.drawable.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet

            1301 to R.drawable.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain
            1302 to R.drawable.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain
            1303 to R.drawable.thunderstorms_day_rain, // single filled out cloud with sun, lightning and rain
            1304 to R.drawable.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain (downpour)
            1305 to R.drawable.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain (downpour)
            1306 to R.drawable.thunderstorms_day_rain, // single filled out cloud with sun, lightning and rain (downpour)
            1307 to R.drawable.thunderstorms_extreme_snow, // two filled out clouds, with lightning and snow or sleet
            1308 to R.drawable.thunderstorms_overcast_snow, // single filled out cloud, with lightning and snow or sleet
            1309 to R.drawable.thunderstorms_day_snow, // single filled out cloud with sun, lightning and snow or sleet

            1401 to R.drawable.extreme_fog, // two clouds with fog
            1402 to R.drawable.overcast_fog, // cloud with fog
            1403 to R.drawable.overcast_fog, // single filled out cloud with fog
            1404 to R.drawable.overcast_fog, // single unfilled cloud with fog
            1405 to R.drawable.extreme_fog, // two clouds with fog and rain (drizzle)
            1406 to R.drawable.overcast_fog, // cloud with fog and rain (drizzle)
            1407 to R.drawable.extreme_fog, // two clouds with fog and rain
            1408 to R.drawable.extreme_day_fog, // two clouds with fog, sun and rain
            1409 to R.drawable.overcast_fog, // cloud with fog and snow or sleet

            1501 to R.drawable.extreme_drizzle, // two clouds and drizzle
            1502 to R.drawable.overcast_drizzle, // cloud and drizzle
            1503 to R.drawable.partly_cloudy_day_drizzle, // cloud, sun and drizzle
            1504 to R.drawable.extreme_rain, // two clouds and rain
            1505 to R.drawable.overcast_rain, // cloud and rain
            1506 to R.drawable.partly_cloudy_day_rain, // cloud, sun and rain
            1507 to R.drawable.extreme_rain, // two clouds and rain (downpour)
            1508 to R.drawable.overcast_rain, // cloud and rain (downpour)
            1509 to R.drawable.partly_cloudy_day_rain, // cloud, sun and rain (downpour)

            1601 to R.drawable.extreme_snow, // two clouds and snow
            1602 to R.drawable.overcast_snow, // cloud and snow
            1603 to R.drawable.partly_cloudy_day_snow, // cloud, sun and snow
            1604 to R.drawable.extreme_snow, // two clouds and more snow
            1605 to R.drawable.overcast_snow, // cloud and more snow
            1606 to R.drawable.partly_cloudy_day_snow, // cloud, sun and more snow
            1607 to R.drawable.extreme_snow, // two clouds, snow and wind
            1608 to R.drawable.overcast_snow, // cloud, sun, snow and wind
            1609 to R.drawable.partly_cloudy_day_snow, // cloud, sun, more snow and wind

            1610 to R.drawable.partly_cloudy_day_sleet, // cloud, sun, sleet and wind
            1611 to R.drawable.extreme_snow, // two clouds, snow and wind
            1612 to R.drawable.overcast_snow, // cloud, snow and wind
            1613 to R.drawable.partly_cloudy_day_snow, // cloud, sun, snow and wind
            1614 to R.drawable.extreme_sleet, // two clouds, and sleet
            1615 to R.drawable.overcast_sleet, // cloud, and sleet
            1616 to R.drawable.partly_cloudy_day_sleet, // cloud, sun, and sleet
            1617 to R.drawable.extreme_sleet, // two clouds, and sleet
            1618 to R.drawable.overcast_sleet, // cloud, and sleet
            1619 to R.drawable.partly_cloudy_day_sleet, // cloud, sun, and sleet

            2101 to R.drawable.clear_night,
            2102 to R.drawable.partly_cloudy_night,
            2103 to R.drawable.overcast_night, // single fulled out cloud with sun
            2104 to R.drawable.extreme_night, // single filled out cloud
            2105 to R.drawable.extreme, // two filled out clouds

            2201 to R.drawable.extreme_sleet, // two filled out cloud with sleet
            2202 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            2203 to R.drawable.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet
            2204 to R.drawable.extreme_sleet, // two filled out cloud with sleet
            2205 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            2206 to R.drawable.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet
            2207 to R.drawable.overcast_sleet, // single filled out cloud with sleet
            2208 to R.drawable.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet

            2301 to R.drawable.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain
            2302 to R.drawable.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain
            2303 to R.drawable.thunderstorms_night_rain, // single filled out cloud with sun, lightning and rain
            2304 to R.drawable.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain (downpour)
            2305 to R.drawable.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain (downpour)
            2306 to R.drawable.thunderstorms_night_rain, // single filled out cloud with sun, lightning and rain (downpour)
            2307 to R.drawable.thunderstorms_extreme_snow, // two filled out clouds, with lightning and snow or sleet
            2308 to R.drawable.thunderstorms_overcast_snow, // single filled out cloud, with lightning and snow or sleet
            2309 to R.drawable.thunderstorms_night_snow, // single filled out cloud with sun, lightning and snow or sleet

            2401 to R.drawable.extreme_fog, // two clouds with fog
            2402 to R.drawable.overcast_fog, // cloud with fog
            2403 to R.drawable.overcast_fog, // single filled out cloud with fog
            2404 to R.drawable.overcast_fog, // single unfilled cloud with fog
            2405 to R.drawable.extreme_fog, // two clouds with fog and rain (drizzle)
            2406 to R.drawable.overcast_fog, // cloud with fog and rain (drizzle)
            2407 to R.drawable.extreme_fog, // two clouds with fog and rain
            2408 to R.drawable.extreme_night_fog, // two clouds with fog, sun and rain
            2409 to R.drawable.overcast_fog, // cloud with fog and snow or sleet

            2501 to R.drawable.extreme_drizzle, // two clouds and drizzle
            2502 to R.drawable.overcast_drizzle, // cloud and drizzle
            2503 to R.drawable.partly_cloudy_night_drizzle, // cloud, sun and drizzle
            2504 to R.drawable.extreme_rain, // two clouds and rain
            2505 to R.drawable.overcast_rain, // cloud and rain
            2506 to R.drawable.partly_cloudy_night_rain, // cloud, sun and rain
            2507 to R.drawable.extreme_rain, // two clouds and rain (downpour)
            2508 to R.drawable.overcast_rain, // cloud and rain (downpour)
            2509 to R.drawable.partly_cloudy_night_rain, // cloud, sun and rain (downpour)

            2601 to R.drawable.extreme_snow, // two clouds and snow
            2602 to R.drawable.overcast_snow, // cloud and snow
            2603 to R.drawable.partly_cloudy_night_snow, // cloud, sun and snow
            2604 to R.drawable.extreme_snow, // two clouds and more snow
            2605 to R.drawable.overcast_snow, // cloud and more snow
            2606 to R.drawable.partly_cloudy_night_snow, // cloud, sun and more snow
            2607 to R.drawable.extreme_snow, // two clouds, snow and wind
            2608 to R.drawable.overcast_snow, // cloud, sun, snow and wind
            2609 to R.drawable.partly_cloudy_night_snow, // cloud, sun, more snow and wind

            2610 to R.drawable.partly_cloudy_night_sleet, // cloud, moon, sleet and wind
            2611 to R.drawable.extreme_snow, // two clouds, snow and wind
            2612 to R.drawable.overcast_snow, // cloud, snow and wind
            2613 to R.drawable.partly_cloudy_night_snow, // cloud, moon, snow and wind
            2614 to R.drawable.extreme_sleet, // two clouds, and sleet
            2615 to R.drawable.overcast_sleet, // cloud, and sleet
            2616 to R.drawable.partly_cloudy_night_sleet, // cloud, moon, and sleet
            2617 to R.drawable.extreme_sleet, // two clouds, and sleet
            2618 to R.drawable.overcast_sleet, // cloud, and sleet
            2619 to R.drawable.partly_cloudy_night_sleet, // cloud, moon, and sleet
        )

        val alternateAnimatedIconMapping: HashMap<Int, Int> = hashMapOf(
            1101 to R.raw.clear_day,
            1102 to R.raw.partly_cloudy_day,
            1103 to R.raw.overcast_day, // single fulled out cloud with sun
            1104 to R.raw.extreme_day, // single filled out cloud
            1105 to R.raw.extreme, // two filled out clouds

            1201 to R.raw.extreme_sleet, // two filled out cloud with sleet
            1202 to R.raw.overcast_sleet, // single filled out cloud with sleet
            1203 to R.raw.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet
            1204 to R.raw.extreme_sleet, // two filled out cloud with sleet
            1205 to R.raw.overcast_sleet, // single filled out cloud with sleet
            1206 to R.raw.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet
            1207 to R.raw.overcast_sleet, // single filled out cloud with sleet
            1208 to R.raw.partly_cloudy_day_sleet, // single filled out cloud with sun and sleet

            1301 to R.raw.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain
            1302 to R.raw.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain
            1303 to R.raw.thunderstorms_day_rain, // single filled out cloud with sun, lightning and rain
            1304 to R.raw.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain (downpour)
            1305 to R.raw.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain (downpour)
            1306 to R.raw.thunderstorms_day_rain, // single filled out cloud with sun, lightning and rain (downpour)
            1307 to R.raw.thunderstorms_extreme_snow, // two filled out clouds, with lightning and snow or sleet
            1308 to R.raw.thunderstorms_overcast_snow, // single filled out cloud, with lightning and snow or sleet
            1309 to R.raw.thunderstorms_day_snow, // single filled out cloud with sun, lightning and snow or sleet

            1401 to R.raw.extreme_fog, // two clouds with fog
            1402 to R.raw.overcast_fog, // cloud with fog
            1403 to R.raw.overcast_fog, // single filled out cloud with fog
            1404 to R.raw.overcast_fog, // single unfilled cloud with fog
            1405 to R.raw.extreme_fog, // two clouds with fog and rain (drizzle)
            1406 to R.raw.overcast_fog, // cloud with fog and rain (drizzle)
            1407 to R.raw.extreme_fog, // two clouds with fog and rain
            1408 to R.raw.extreme_day_fog, // two clouds with fog, sun and rain
            1409 to R.raw.overcast_fog, // cloud with fog and snow or sleet

            1501 to R.raw.extreme_drizzle, // two clouds and drizzle
            1502 to R.raw.overcast_drizzle, // cloud and drizzle
            1503 to R.raw.partly_cloudy_day_drizzle, // cloud, sun and drizzle
            1504 to R.raw.extreme_rain, // two clouds and rain
            1505 to R.raw.overcast_rain, // cloud and rain
            1506 to R.raw.partly_cloudy_day_rain, // cloud, sun and rain
            1507 to R.raw.extreme_rain, // two clouds and rain (downpour)
            1508 to R.raw.overcast_rain, // cloud and rain (downpour)
            1509 to R.raw.partly_cloudy_day_rain, // cloud, sun and rain (downpour)

            1601 to R.raw.extreme_snow, // two clouds and snow
            1602 to R.raw.overcast_snow, // cloud and snow
            1603 to R.raw.partly_cloudy_day_snow, // cloud, sun and snow
            1604 to R.raw.extreme_snow, // two clouds and more snow
            1605 to R.raw.overcast_snow, // cloud and more snow
            1606 to R.raw.partly_cloudy_day_snow, // cloud, sun and more snow
            1607 to R.raw.extreme_snow, // two clouds, snow and wind
            1608 to R.raw.overcast_snow, // cloud, sun, snow and wind
            1609 to R.raw.partly_cloudy_day_snow, // cloud, sun, more snow and wind

            1610 to R.raw.partly_cloudy_day_sleet, // cloud, sun, sleet and wind
            1611 to R.raw.extreme_snow, // two clouds, snow and wind
            1612 to R.raw.overcast_snow, // cloud, snow and wind
            1613 to R.raw.partly_cloudy_day_snow, // cloud, sun, snow and wind
            1614 to R.raw.extreme_sleet, // two clouds, and sleet
            1615 to R.raw.overcast_sleet, // cloud, and sleet
            1616 to R.raw.partly_cloudy_day_sleet, // cloud, sun, and sleet
            1617 to R.raw.extreme_sleet, // two clouds, and sleet
            1618 to R.raw.overcast_sleet, // cloud, and sleet
            1619 to R.raw.partly_cloudy_day_sleet, // cloud, sun, and sleet

            2101 to R.raw.clear_night,
            2102 to R.raw.partly_cloudy_night,
            2103 to R.raw.overcast_night, // single fulled out cloud with sun
            2104 to R.raw.extreme_night, // single filled out cloud
            2105 to R.raw.extreme, // two filled out clouds

            2201 to R.raw.extreme_sleet, // two filled out cloud with sleet
            2202 to R.raw.overcast_sleet, // single filled out cloud with sleet
            2203 to R.raw.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet
            2204 to R.raw.extreme_sleet, // two filled out cloud with sleet
            2205 to R.raw.overcast_sleet, // single filled out cloud with sleet
            2206 to R.raw.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet
            2207 to R.raw.overcast_sleet, // single filled out cloud with sleet
            2208 to R.raw.partly_cloudy_night_sleet, // single filled out cloud with sun and sleet

            2301 to R.raw.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain
            2302 to R.raw.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain
            2303 to R.raw.thunderstorms_night_rain, // single filled out cloud with sun, lightning and rain
            2304 to R.raw.thunderstorms_extreme_rain, // two filled out clouds, with lightning and rain (downpour)
            2305 to R.raw.thunderstorms_overcast_rain, // single filled out cloud, with lightning and rain (downpour)
            2306 to R.raw.thunderstorms_night_rain, // single filled out cloud with sun, lightning and rain (downpour)
            2307 to R.raw.thunderstorms_extreme_snow, // two filled out clouds, with lightning and snow or sleet
            2308 to R.raw.thunderstorms_overcast_snow, // single filled out cloud, with lightning and snow or sleet
            2309 to R.raw.thunderstorms_night_snow, // single filled out cloud with sun, lightning and snow or sleet

            2401 to R.raw.extreme_fog, // two clouds with fog
            2402 to R.raw.overcast_fog, // cloud with fog
            2403 to R.raw.overcast_fog, // single filled out cloud with fog
            2404 to R.raw.overcast_fog, // single unfilled cloud with fog
            2405 to R.raw.extreme_fog, // two clouds with fog and rain (drizzle)
            2406 to R.raw.overcast_fog, // cloud with fog and rain (drizzle)
            2407 to R.raw.extreme_fog, // two clouds with fog and rain
            2408 to R.raw.extreme_night_fog, // two clouds with fog, sun and rain
            2409 to R.raw.overcast_fog, // cloud with fog and snow or sleet

            2501 to R.raw.extreme_drizzle, // two clouds and drizzle
            2502 to R.raw.overcast_drizzle, // cloud and drizzle
            2503 to R.raw.partly_cloudy_night_drizzle, // cloud, sun and drizzle
            2504 to R.raw.extreme_rain, // two clouds and rain
            2505 to R.raw.overcast_rain, // cloud and rain
            2506 to R.raw.partly_cloudy_night_rain, // cloud, sun and rain
            2507 to R.raw.extreme_rain, // two clouds and rain (downpour)
            2508 to R.raw.overcast_rain, // cloud and rain (downpour)
            2509 to R.raw.partly_cloudy_night_rain, // cloud, sun and rain (downpour)

            2601 to R.raw.extreme_snow, // two clouds and snow
            2602 to R.raw.overcast_snow, // cloud and snow
            2603 to R.raw.partly_cloudy_night_snow, // cloud, sun and snow
            2604 to R.raw.extreme_snow, // two clouds and more snow
            2605 to R.raw.overcast_snow, // cloud and more snow
            2606 to R.raw.partly_cloudy_night_snow, // cloud, sun and more snow
            2607 to R.raw.extreme_snow, // two clouds, snow and wind
            2608 to R.raw.overcast_snow, // cloud, sun, snow and wind
            2609 to R.raw.partly_cloudy_night_snow, // cloud, sun, more snow and wind

            2610 to R.raw.partly_cloudy_night_sleet, // cloud, sun, sleet and wind
            2611 to R.raw.extreme_snow, // two clouds, snow and wind
            2612 to R.raw.overcast_snow, // cloud, snow and wind
            2613 to R.raw.partly_cloudy_night_snow, // cloud, sun, snow and wind
            2614 to R.raw.extreme_sleet, // two clouds, and sleet
            2615 to R.raw.overcast_sleet, // cloud, and sleet
            2616 to R.raw.partly_cloudy_night_sleet, // cloud, sun, and sleet
            2617 to R.raw.extreme_sleet, // two clouds, and sleet
            2618 to R.raw.overcast_sleet, // cloud, and sleet
            2619 to R.raw.partly_cloudy_night_sleet, // cloud, sun, and sleet
        )

        val rainCodes: List<Int> = listOf(
            // day
            1201, 1202, 1203, 1204, 1205, 1206, 1207, 1208, // sleet
            1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, // rain and sleet
            1405, 1406, 1407, 1408, 1409, // fog an drizzle, rain or sleet
            1501, 1502, 1503, 1504, 1505, 1506, 1507, 1508, 1509, // rain
            1601, 1602, 1603, 1604, 1605, 1606, 1607, 1608, 1609, // snow
            1610, 1611, 1612, 1613, 1614, 1615, 1616, 1617, 1618, 1619, // sleet or snow
            // night
            2201, 2202, 2203, 2204, 2205, 2206, 2207, 2208, // sleet
            2301, 2302, 2303, 2304, 2305, 2306, 2307, 2308, 2309, // rain and sleet
            2405, 2406, 2407, 2408, 2409, // fog an drizzle, rain or sleet
            2501, 2502, 2503, 2504, 2505, 2506, 2507, 2508, 2509, // rain
            2601, 2602, 2603, 2604, 2605, 2606, 2607, 2608, 2609, // snow
            2610, 2611, 2612, 2613, 2614, 2615, 2616, 2617, 2618, 2619, // sleet or snow
        )

        // TODO: make warning specific icons
        val warningIconMapping: HashMap<String, Int> = hashMapOf(
            "Yellow" to R.drawable.baseline_warning_yellow_24,
            "Orange" to R.drawable.baseline_warning_orange_24,
            "Red" to R.drawable.baseline_warning_24_red
        )
    }
}