package com.example.service

import com.example.data.model.ConflictInfo
import com.example.data.model.ConflictType
import com.example.data.model.Leader
import com.example.data.model.ScheduleEvent

object ConflictDetector {

    /**
     * Checks conflicts for an event against all existing events.
     * Ignore comparison against itself if editing (excludeEventId).
     */
    fun checkConflicts(
        targetEvent: ScheduleEvent,
        allEvents: List<ScheduleEvent>,
        leaders: List<Leader>,
        excludeEventId: String? = null
    ): List<ConflictInfo> {
        val conflicts = mutableListOf<ConflictInfo>()
        val leaderMap = leaders.associateBy { it.id }

        val targetLeaderIds = mutableSetOf(targetEvent.primaryLeaderId)
        if (targetEvent.coAttendees.isNotBlank()) {
            targetLeaderIds.addAll(targetEvent.coAttendees.split(",").map { it.trim() }.filter { it.isNotEmpty() })
        }

        val targetTimeClean = normalizeTime(targetEvent.time)
        val targetLocationClean = targetEvent.location.trim().lowercase()

        for (other in allEvents) {
            if (other.id == (excludeEventId ?: targetEvent.id)) continue
            if (other.date != targetEvent.date) continue

            val otherTimeClean = normalizeTime(other.time)
            val isSameTime = targetTimeClean.isNotBlank() && otherTimeClean.isNotBlank() && targetTimeClean == otherTimeClean

            if (isSameTime) {
                // 1. Check Leader Overlap
                val otherLeaderIds = mutableSetOf(other.primaryLeaderId)
                if (other.coAttendees.isNotBlank()) {
                    otherLeaderIds.addAll(other.coAttendees.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                }

                val overlappingLeaders = targetLeaderIds.intersect(otherLeaderIds)
                if (overlappingLeaders.isNotEmpty()) {
                    val leaderNames = overlappingLeaders.joinToString(", ") { id ->
                        leaderMap[id]?.shortName ?: id
                    }
                    conflicts.add(
                        ConflictInfo(
                            type = ConflictType.LEADER_CONFLICT,
                            message = "Trùng lịch: $leaderNames đã có lịch lúc ${other.time} (\"${other.title.take(60)}...\")",
                            conflictingEventTitle = other.title,
                            conflictingEventId = other.id
                        )
                    )
                }

                // 2. Check Room / Location Overlap
                if (targetLocationClean.isNotBlank() && targetLocationClean != "trụ sở catp" && targetLocationClean != "trụ sở ubnd thành phố") {
                    val otherLocationClean = other.location.trim().lowercase()
                    if (otherLocationClean == targetLocationClean) {
                        conflicts.add(
                            ConflictInfo(
                                type = ConflictType.ROOM_CONFLICT,
                                message = "Trùng phòng họp: Địa điểm \"${targetEvent.location}\" đã được bố trí cho cuộc họp khác lúc ${other.time}",
                                conflictingEventTitle = other.title,
                                conflictingEventId = other.id
                            )
                        )
                    }
                }
            }
        }

        return conflicts
    }

    private fun normalizeTime(t: String): String {
        return t.replace(" ", "").lowercase().replace(":", "h")
    }
}
