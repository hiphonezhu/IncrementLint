package com.jianghongkui.lib_rules.detector

import com.android.SdkConstants.*
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.XmlContext
import com.jianghongkui.lib_rules.FormatIssue
import com.jianghongkui.lib_rules.LayoutLevelChecker
import com.jianghongkui.lib_rules.LayoutLevelChecker.IActionListener
import com.jianghongkui.lib_rules.UsageIssue
import com.jianghongkui.lib_rules.base.BaseDetector
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File

/**
 * 处理布局文件层次相关规则
 *
 * @author jianghongkui
 * @date 2018/10/10
 */
open class LayoutLevelDetector : BaseDetector.XmlDetector() {

    private lateinit var checker: LayoutLevelChecker
    private lateinit var elements: ArrayList<Element>
    private lateinit var elementLevels: ArrayList<Int>

    companion object {
        const val CRITICAL_LEVEL = 4
        val data: HashMap<String, Int> = HashMap()
    }

    override fun beforeCheckFile(context: Context) {
        super.beforeCheckFile(context)
        elements = ArrayList(CRITICAL_LEVEL)
        elementLevels = ArrayList(CRITICAL_LEVEL)
        checker = LayoutLevelChecker(object : IActionListener {
            override fun onVisitElement(element: Element, level: Int, hasChild: Boolean, isStartByMerge: Boolean): Boolean {
                if (context is XmlContext) {
                    checkFormat(context, element, level)
                    if (level > CRITICAL_LEVEL && hasChild) {
                        context.report(UsageIssue.ISSUE_LAYOUT, element, context.getLocation(element), "层数不应超过3")
                    }
                    if (element.nodeName == TAG_INCLUDE || element.tagName == VIEW_STUB) {
                        elements.add(element)
                        elementLevels.add(level - 1)
                    }
                }
                return super.onVisitElement(element, level, hasChild, isStartByMerge)
            }
        })
    }

    override fun afterCheckFile(context: Context) {
        super.afterCheckFile(context)
        if (context is XmlContext) {
            var i = 0
            while (i < elements.size) {
                visitChildFile(context, elements[i], elementLevels[i])
                i++
            }
        }
        elementLevels.clear()
        elements.clear()

    }

    override fun visitDocument(context: XmlContext, document: Document) {
        checker.start(document = document)
    }

    private fun checkFormat(context: XmlContext, element: Element, level: Int) {
        var needColumn = level * 4 - 4

        val location = context.getLocation(element)
        val column = location.start!!.column
        if (column != needColumn) {
            context.report(FormatIssue.ISSUE_LAYOUT, element, location, "1格式有误")
            return
        }
        val map = element.attributes
        needColumn = column + 4
        var node: Node
        var attrLocation: Location
        for (i in 0 until map.length) {
            node = map.item(i)
            if (node.nodeName.startsWith(XMLNS_ANDROID))
                continue
            attrLocation = context.getLocation(node)
            if (needColumn != attrLocation.start!!.column) {
                context.report(FormatIssue.ISSUE_LAYOUT, node, attrLocation, "2格式有误")
            }
        }
    }

    private fun visitChildFile(context: XmlContext, element: Element, level: Int) {
        var attrStr = "layout"
        if (element.tagName == VIEW_STUB) attrStr = "android:layout"
        if (!element.hasAttribute(attrStr)) return

        val fileName = element.getAttribute(attrStr).substringAfter('/')
        if (data.containsKey(fileName) && (data[fileName]!! + level) > CRITICAL_LEVEL) {
            context.report(UsageIssue.ISSUE_LAYOUT, element, context.getLocation(element), "层数不应超过3")
            return
        }
        val localChecker = LayoutLevelChecker(object : IActionListener {

            override fun afterCheck(maxLevel: Int, isStartByMerge: Boolean) {
                var realLevel = maxLevel
                if (isStartByMerge)
                    realLevel = maxLevel - 1
                if (level + realLevel - 1 > CRITICAL_LEVEL) {
                    context.report(UsageIssue.ISSUE_LAYOUT, element, context.getLocation(element), "层数不应超过3")
                }
                if (realLevel > 1) {
                    data[fileName] = realLevel - 1
                } else {
                    data[fileName] = 0
                }
            }
        })
        localChecker.start(File(context.file.parentFile, "$fileName.xml"))
    }
}