package com.groops.fairsquare.models


interface Scorable {
    fun getScore(): Double
}

class ScoreComparator {

    companion object: Comparator<Scorable> {
        override fun compare(o1: Scorable?, o2: Scorable?): Int {
            return if (o2 != null && o1 != null) o2.getScore().compareTo(o1.getScore()) else 0
        }
    }

}