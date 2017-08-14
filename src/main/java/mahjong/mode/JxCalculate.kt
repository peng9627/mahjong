@file:Suppress("UNCHECKED_CAST")

package mahjong.mode

/**
 * Created by gcxk721 on 2016/12/20.
 */
class JxCalculate(private val handList: ArrayList<Int>, private val lai_majong: Int) {

    /**
     * 癞子牌的计算数据
     */
    private val calMap = HashMap<Int, JxCalculateData>()

    private var general = false

    private var c_huList = ArrayList<Int>()

    fun calculateHu() {
        general = false
//        if (!gameData.islaizi)
//            super.calculateHu()
//        else {
        val partition = handList.partition { it == lai_majong }
        if (partition.first.isEmpty()) {
//                super.calculateHu()
//                if (c_huList.isNotEmpty()) {
//                    hunMajong(player.handList, gameData.lai_majong, gameData.lai_majong, 1)
//                    c_huList.add(gameData.lai_majong)
//                }
        } else {
            val now = System.currentTimeMillis()
            calMap.clear()
            c_huList.clear()
            //剔除混子牌
            val handlist = partition.second as ArrayList<Int>
            if (handlist.isEmpty() && partition.first.size == 1) {
                //只有一张癞子牌
                general = true
                return
            }
            handlist.sort()
            val possible = MahjongUtil.getComputePossible(handlist, 2)
            possible.remove(lai_majong)
            hunMajong(handlist, possible, lai_majong, partition.first.size)
            if (calMap.isNotEmpty())
                hunMajong(handlist, lai_majong, lai_majong, partition.first.size + 1)
            calMap.map {
                c_huList.add(it.key)
                print("win: " + it.value.hu_majong)
                print("  ===  ")
                print(it.value.list)
                print("  ===  ")
                println(it.value.replace)
            }
            println("time: " + (System.currentTimeMillis() - now))
        }
//        }
    }

    private fun hunMajong(handlist: ArrayList<Int>, hu_majong: Int, hun_majong: Int, num: Int) {
        val count = MahjongUtil.findPairNumber(handlist)
        if (count + num > 6) {
            val calData = JxCalculateData()
            calData.hu_majong = hu_majong
            handlist.map {
                val majong = it
                calData.list.add(majong)
                val c = handlist.count { it == majong }
                if (c == 1 || c == 3) {
                    if (!calData.replace.contains(majong)) {
                        calData.replace.add(majong)
                        calData.list.add(majong)
                    }
                }
            }
            while (calData.list.size < 14)
                calData.list.add(hun_majong)
            calData.list.sort()
            calMap.put(hu_majong, calData)
        } else {
            handlist.sort()
            //所有能做将的可能
            val pairs = ArrayList<Int>()
            handlist.map {
                if (!pairs.contains(it))
                    pairs.add(it)
            }
            val list = ArrayList<Int>()
            val replace = ArrayList<Int>()
            for (pair in pairs) {
                var hun = num
                replace.clear()
                if (handlist.count { it == pair } == 1) {
                    if (hun == 0) return
                    replace.add(pair)
                    hun--
                }
                copylist(handlist, list, pair)
                if (lug(list, replace, hun)) {
                    val calData = JxCalculateData()
                    calData.hu_majong = hu_majong
                    calData.list.addAll(replace)
                    calData.list.addAll(handlist)
                    calData.list.sort()
                    calData.replace.addAll(replace)
                    calMap[hu_majong] = calData
                    return
                }
            }
        }
        return
    }

    private fun hunMajong(handlist: ArrayList<Int>, possible: ArrayList<Int>, hun_majong: Int, num: Int) {
        //进入这个方法，就代表手上一定有混牌  留一张混牌做将，先测试万能胡牌
        val count = MahjongUtil.findPairNumber(handlist)
        if (count + num > 6) {
            //7对的万能牌
            general = true
            return
        }
        //留下一张混牌当将，万能胡
        if (lug(handlist.clone() as ArrayList<Int>, ArrayList<Int>(), num - 1)) {
            general = true
            return
        }
        possible.map {
            handlist.add(it)
            hunMajong(handlist, it, hun_majong, num)
            handlist.remove(it)
        }
    }

    //同一颜色，不是风牌
    private fun subLug(majong: Int, list: ArrayList<Int>, replace: ArrayList<Int>, hun: Int): Boolean {
        if (list.isEmpty()) {
            if (hun > 1) {
                replace.add(majong)
                replace.add(majong)
                return true
            }
            return false
        }
        if (list.size == 1) {
            if (hun > 0) {
                val only = list.removeAt(0)
                if (majong == only) {
                    replace.add(majong)
                    return true
                }
                if (only - majong < 3) {
                    if (majong + 1 == only.toInt()) {
                        if (Card.legal(only + 1))
                            replace.add(only + 1)
                        else
                            replace.add(majong - 1)
                    } else
                        replace.add(majong + 1)
                    return true
                }
            }
            return false
        }
        val count = list.count { it == majong }
        when (count) {
            0 -> {
                //一定要连成顺子 否则消耗2张混牌
                val next = list[0]
                if (next - majong < 3) {
                    if (majong + 2 == next.toInt()) {
                        if (hun > 0) {
                            replace.add(majong + 1)
                            list.remove(next)
                            return subLug(list.removeAt(0), list, replace, hun - 1)
                        } else return false
                    } else {
                        //两张牌相邻
                        val want = next + 1
                        if (Card.legal(want)) {
                            if (list.contains(want)) {
                                list.remove(next)
                                list.remove(want)
                                if (list.isNotEmpty())
                                    return subLug(list.removeAt(0), list, replace, hun)
                                return true
                            } else {
                                if (hun > 0) {
                                    replace.add(want)
                                    list.remove(next)
                                    return subLug(list.removeAt(0), list, replace, hun - 1)
                                } else return false
                            }
                        } else {
                            if (hun > 0) {
                                replace.add(majong - 1)
                                list.remove(next)
                                return subLug(list.removeAt(0), list, replace, hun - 1)
                            } else return false
                        }
                    }
                } else {
                    if (hun > 1) {
                        replace.add(majong)
                        replace.add(majong)
                        return subLug(list.removeAt(0), list, replace, hun - 2)
                    }
                }
                if (list.contains(majong + 1)) {
                    if (list.contains(majong + 2)) {
                        list.remove(majong + 1)
                        list.remove(majong + 2)
                        if (list.isEmpty()) return true
                        return subLug(list.removeAt(0), list, replace, hun)
                    } else {
                        if (hun > 0) {
                            list.remove(majong + 1)
                            if (subLug(list.removeAt(0), list, replace, hun - 1)) {
                                val hun_majong = majong + 2
                                if (Card.legal(hun_majong))
                                    replace.add(hun_majong)
                                else
                                    replace.add(majong - 1)
                            }
                        }
                        return false
                    }
                }
            }
            1 -> {
                //两种可能，混牌成3张或者找顺子，都要判断
                val next = list[1]
                if (next - majong < 3) {//有顺子的可能
                    if (majong + 2 == next.toInt()) {
                        if (hun > 0) {
                            val clist = list.clone() as ArrayList<Int>
                            clist.remove(next)
                            if (subLug(clist.removeAt(0), clist, replace, hun - 1)) {
                                replace.add(majong + 1)
                                return true
                            }
                        }
                    } else {
                        //两张牌相邻
                        val want = next + 1
                        if (Card.legal(want)) {
                            val clist = list.clone() as ArrayList<Int>
                            if (clist.contains(want)) {
                                clist.remove(next)
                                clist.remove(want)
                                if (subLug(clist.removeAt(0), clist, replace, hun))
                                    return true
                            } else {
                                if (hun > 0) {
                                    clist.remove(next)
                                    if (subLug(clist.removeAt(0), clist, replace, hun - 1)) {
                                        replace.add(want)
                                        return true
                                    }
                                }
                            }
                        } else {
                            if (hun > 0) {
                                val clist = list.clone() as ArrayList<Int>
                                clist.remove(next)
                                if (subLug(clist.removeAt(0), clist, replace, hun - 1)) {
                                    replace.add(majong - 1)
                                    return true
                                }
                            }
                        }
                    }
                }
                //没法凑顺子，直接3张
                if (hun > 0) {
                    list.removeAt(0)
                    replace.add(majong)
                    return subLug(list.removeAt(0), list, replace, hun - 1)
                } else return false
            }
            else -> {
                list.remove(majong)
                list.remove(majong)
                if (list.isNotEmpty())
                    return subLug(list.removeAt(0), list, replace, hun)
                return true
            }
        }
        return false
    }

    private fun subFengLug(majong: Int, list: ArrayList<Int>, replace: ArrayList<Int>, hun: Int): Boolean {
        val count = list.count { it == majong }
        when (count) {
            0 -> {
                if (hun > 1) {
                    replace.add(majong)
                    replace.add(majong)
                    if (list.isNotEmpty())
                        return subFengLug(list.removeAt(0), list, replace, hun - 2)
                    return true
                }
            }
            1 -> {
                if (hun > 0) {
                    replace.add(majong)
                    list.remove(majong)
                    if (list.isNotEmpty())
                        return subFengLug(list.removeAt(0), list, replace, hun - 1)
                    return true
                }
            }
            else -> {
                list.remove(majong)
                list.remove(majong)
                if (list.isNotEmpty())
                    return subFengLug(list.removeAt(0), list, replace, hun)
                return true
            }
        }
        return false
    }

    private fun lug(list: ArrayList<Int>, replace: ArrayList<Int>, hun: Int): Boolean {
        if (list.isEmpty()) return true
        val majong = list.removeAt(0)
        val color = majong / 10
        val partition = list.partition { it / 10 == color }
        val sublist = partition.first as ArrayList<Int>
        val number = hun + replace.size
        if (2 < color) {
            if (subFengLug(majong, sublist, replace, hun))
                return lug(partition.second as ArrayList<Int>, replace, number - replace.size)
        } else {
            if (subLug(majong, sublist, replace, hun))
                return lug(partition.second as ArrayList<Int>, replace, number - replace.size)
        }
        return false
    }

    private fun copylist(source: ArrayList<Int>, target: ArrayList<Int>, majong: Int) {
        var number = 2
        target.clear()
        source.map {
            if (it == majong && number > 0) number -= 1
            else target.add(it)
        }
    }
}

class JxCalculateData {
    /**
     * 胡的那张牌
     */
    var hu_majong: Int = 0

    /**
     * 胡牌是对应的手牌
     */
    val list = ArrayList<Int>()

    /**
     * 替换的牌
     */
    val replace = ArrayList<Int>()

}

