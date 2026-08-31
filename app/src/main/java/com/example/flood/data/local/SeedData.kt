package com.example.flood.data.local

import com.example.flood.data.model.DisasterSimulation
import com.example.flood.data.model.EmergencyService
import com.example.flood.data.model.Incident

object SeedData {
    val INITIAL_INCIDENTS = listOf(
        Incident(
            id = 1,
            type = "yellow",
            note = "High river discharge reported along Bhagirathi bank",
            lat = 30.7298,
            lng = 78.4398,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 10,
            severity = "MEDIUM",
            userReported = false,
            score = 1
        ),
        Incident(
            id = 2,
            type = "road",
            note = "Road blocked due to landslide on NH-34 bend",
            lat = 30.8075,
            lng = 78.5672,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 8,
            severity = "HIGH",
            userReported = false,
            score = 0
        ),
        Incident(
            id = 3,
            type = "distress",
            note = "Bridge approach washed out; urgent evacuation assistance required",
            lat = 30.7535,
            lng = 78.7350,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 6,
            severity = "HIGH",
            userReported = false,
            score = 0
        ),
        Incident(
            id = 4,
            type = "flood",
            note = "Low lying market waterlogged up to 3 feet",
            lat = 30.7922,
            lng = 78.4621,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 5,
            severity = "HIGH",
            userReported = false,
            score = 2
        ),
        Incident(
            id = 5,
            type = "tree",
            note = "Fallen pine tree blocking both lanes near bridge",
            lat = 30.7240,
            lng = 78.4335,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
            severity = "MEDIUM",
            userReported = false,
            score = 0
        ),
        Incident(
            id = 6,
            type = "distress",
            note = "Evacuation required near Joshiyara low ground",
            lat = 30.7214,
            lng = 78.4421,
            createdAt = System.currentTimeMillis() - 1000 * 60 * 60 * 2,
            severity = "HIGH",
            userReported = false,
            score = 1
        )
    )

    val EMERGENCY_SERVICES = listOf(
        // Hospitals
        EmergencyService(
            id = "h1",
            type = "hospital",
            name = "District Hospital Uttarkashi",
            lat = 30.7289,
            lng = 78.4356,
            address = "NH-34, Main Road, Uttarkashi, Uttarakhand",
            phone = "+91-1374-222102"
        ),
        EmergencyService(
            id = "h2",
            type = "hospital",
            name = "Community Health Centre (CHC) Bhatwari",
            lat = 30.8060,
            lng = 78.5660,
            address = "Bhatwari Tehsil, Uttarkashi",
            phone = "+91-1374-245220"
        ),
        EmergencyService(
            id = "h3",
            type = "hospital",
            name = "Primary Health Centre (PHC) Dharali",
            lat = 30.8933,
            lng = 79.0703,
            address = "Dharali Market, Uttarkashi",
            phone = "+91-1374-222340"
        ),
        EmergencyService(
            id = "h4",
            type = "hospital",
            name = "Harsil Army Medical Unit",
            lat = 30.7508,
            lng = 78.7326,
            address = "Army Cantonment Base, Harsil",
            phone = "+91-1374-282100"
        ),
        EmergencyService(
            id = "h5",
            type = "hospital",
            name = "Maneri Health Center",
            lat = 30.7922,
            lng = 78.4621,
            address = "Near Maneri Dam Colony, Maneri",
            phone = "+91-1374-232115"
        ),
        EmergencyService(
            id = "h6",
            type = "hospital",
            name = "Purola Community Health Clinic",
            lat = 30.8838,
            lng = 78.0710,
            address = "Main Bazar, Purola, Uttarkashi",
            phone = "+91-1374-266108"
        ),

        // Shelters
        EmergencyService(
            id = "s1",
            type = "shelter",
            name = "Uttarkashi Govt School Shelter",
            lat = 30.7298,
            lng = 78.4398,
            address = "Govt Inter College Grounds, Uttarkashi",
            phone = "+91-1374-222111"
        ),
        EmergencyService(
            id = "s2",
            type = "shelter",
            name = "Dharali Community Hall",
            lat = 30.8911,
            lng = 79.0631,
            address = "Near Main Market, Dharali",
            phone = "+91-1374-222144"
        ),
        EmergencyService(
            id = "s3",
            type = "shelter",
            name = "Bhatwari Panchayat Bhawan",
            lat = 30.8075,
            lng = 78.5672,
            address = "Gram Panchayat Centre, Bhatwari",
            phone = "+91-1374-222155"
        ),
        EmergencyService(
            id = "s4",
            type = "shelter",
            name = "Harsil GMVN Relief Camp",
            lat = 30.7535,
            lng = 78.7350,
            address = "GMVN Tourist Complex, Harsil",
            phone = "+91-1374-282110"
        ),
        EmergencyService(
            id = "s5",
            type = "shelter",
            name = "Maneri School Relief Shelter",
            lat = 30.7935,
            lng = 78.4630,
            address = "Primary School Campus, Maneri",
            phone = "+91-1374-222166"
        ),
        EmergencyService(
            id = "s6",
            type = "shelter",
            name = "Joshiyara Disaster Relief Center",
            lat = 30.7214,
            lng = 78.4421,
            address = "Joshiyara Stadium Ground, Uttarkashi",
            phone = "+91-1374-222177"
        ),

        // Police & Rescue
        EmergencyService(
            id = "p1",
            type = "police",
            name = "Uttarkashi Central Police Station & SDRF",
            lat = 30.7310,
            lng = 78.4370,
            address = "Police Lines, Kotwali Uttarkashi",
            phone = "112"
        ),
        EmergencyService(
            id = "p2",
            type = "police",
            name = "Bhatwari Police Outpost",
            lat = 30.8090,
            lng = 78.5690,
            address = "Bhatwari Checkpost",
            phone = "112"
        ),
        EmergencyService(
            id = "p3",
            type = "police",
            name = "Harsil Police & ITBP Unit",
            lat = 30.7550,
            lng = 78.7380,
            address = "Harsil Border Checkpoint",
            phone = "112"
        )
    )

    val SIMULATIONS = listOf(
        DisasterSimulation(
            id = "2013",
            title = "2013 Flash Flood Re-enactment",
            year = "2013",
            description = "Simulating the catastrophic 2013 Uttarakhand cloudburst and Bhagirathi surge. Severe widespread flooding and landslides across Dharali and Uttarkashi valleys.",
            rainIntensity = 95,
            riskColor = "red",
            simulatedIncidents = listOf(
                Incident(id = 101, type = "flood", note = "Bhagirathi river overflowed embankments", lat = 30.7280, lng = 78.4340, severity = "HIGH"),
                Incident(id = 102, type = "landslide", note = "Massive rockfall cut off NH-34 at Bhatwari", lat = 30.8120, lng = 78.5710, severity = "HIGH"),
                Incident(id = 103, type = "distress", note = "Over 150 pilgrims stranded near Dharali", lat = 30.8920, lng = 79.0680, severity = "HIGH"),
                Incident(id = 104, type = "road", note = "Main bridge approaches washed away", lat = 30.7350, lng = 78.4420, severity = "HIGH")
            )
        ),
        DisasterSimulation(
            id = "2021",
            title = "2021 Glacial Burst Scenario",
            year = "2021",
            description = "Simulating downstream flash surge and debris flow caused by high-altitude glacial rupture and heavy precipitation.",
            rainIntensity = 75,
            riskColor = "orange",
            simulatedIncidents = listOf(
                Incident(id = 201, type = "flood", note = "Rapid water level rise (+4.2m) at Dam basin", lat = 30.7930, lng = 78.4610, severity = "HIGH"),
                Incident(id = 202, type = "road", note = "Debris accumulation at tunnel inlet", lat = 30.7850, lng = 78.4550, severity = "HIGH"),
                Incident(id = 203, type = "tree", note = "Uprooted trees choking culverts", lat = 30.7220, lng = 78.4310, severity = "MEDIUM")
            )
        )
    )
}
