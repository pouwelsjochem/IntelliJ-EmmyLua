/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.comment.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.comment.LuaCommentUtil
import com.tang.intellij.lua.comment.psi.LuaDocClassNameRef
import com.tang.intellij.lua.comment.psi.LuaDocFunctionTy
import com.tang.intellij.lua.comment.psi.LuaDocGenericDef
import com.tang.intellij.lua.comment.psi.LuaDocPsiElement
import com.tang.intellij.lua.psi.LuaCommentOwner
import com.tang.intellij.lua.psi.LuaElementFactory
import com.tang.intellij.lua.psi.LuaFuncBodyOwner
import com.tang.intellij.lua.psi.LuaParametersOwner
import com.tang.intellij.lua.psi.search.LuaShortNamesManager
import com.tang.intellij.lua.search.SearchContext

/**

 * Created by TangZX on 2016/11/29.
 */
class LuaClassNameReference(element: LuaDocClassNameRef) : PsiReferenceBase<LuaDocClassNameRef>(element) {

    override fun getRangeInElement() = TextRange(0, myElement.textLength)

    override fun isReferenceTo(element: PsiElement): Boolean {
        return myElement.manager.areElementsEquivalent(element, resolve())
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val element = LuaElementFactory.createWith(myElement.project, "---@type $newElementName")
        val classNameRef = PsiTreeUtil.findChildOfType(element, LuaDocClassNameRef::class.java)
        return myElement.replace(classNameRef!!)
    }

    override fun resolve(): PsiElement? {
        val genericTypeName = myElement.text

        val existingTypeDef = LuaShortNamesManager.getInstance(myElement.project).findTypeDef(genericTypeName, SearchContext.get(myElement.project))
        if (existingTypeDef != null) {
            return existingTypeDef
        } else {
            val funcBodyOwnerOfElement = PsiTreeUtil.getParentOfType(element, LuaFuncBodyOwner::class.java)
            return if (funcBodyOwnerOfElement != null) {
                resolveGenericTypeDefRecursive(genericTypeName, funcBodyOwnerOfElement)
            } else {
                null
            }
        }
    }

    private fun resolveGenericTypeDefRecursive(genericTypeName: String, element: LuaFuncBodyOwner): PsiElement? {
        if (element is LuaCommentOwner) {
            var genericDefList = element.comment?.findTags(LuaDocGenericDef::class.java)
            if (genericDefList != null) {
                for (genericDef in genericDefList) {
                    if (genericDef.name == genericTypeName)
                        return genericDef
                }
            }
        }

        val parentFuncBodyOwner = PsiTreeUtil.getParentOfType(element, LuaFuncBodyOwner::class.java)
        return if (parentFuncBodyOwner != null) {
            resolveGenericTypeDefRecursive(genericTypeName, parentFuncBodyOwner)
        } else {
            null;
        }
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
