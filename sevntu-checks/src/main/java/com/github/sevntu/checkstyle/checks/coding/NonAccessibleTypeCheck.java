////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2011  Oliver Burn
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////
package com.github.sevntu.checkstyle.checks.coding;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.api.Check;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * <p>
 * Check prohibits return the types specified via the regular expression outside the 
 * current class.
 * </p>
 * <p>
 * Example: 
 * </p>
 * <p>
 * Return Logger out of the class is the very bad practice as it produce logs that are hard
 * to investigate as logging class does not contains that code and search should be done in 
 * other classes or in hierarchy (if filed is public or accessible by other protected or package).
 * <pre>
 * <code>
 * package com.test;
 * 
 * import org.slf4j.Logger;
 * import org.slf4j.LoggerFactory;
 * 
 * public class Check {
 * 
 *     private Logger log = LoggerFactory.getLogger(getClass());
 * 
 *     protected Logger log2 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     public Logger log3 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     Logger log4 = LoggerFactory.getLogger(getClass()); //Violation
 * 
 *     public Logger getLog() { //Violation
 *         return log;
 *     }
 * }
 * </code>
 * </pre>
 * <p> 
 * The regular expression must contain a qualified type or several types separated by "|".
 * </p>
 * <p>
 * Example:
 * <code>
 * <pre>
 *     java.util.regex.Pattern
 * or
 *     java.util.regex.Pattern|java.util.regex.Matcher|java.util.List
 * </pre>
 * </code>
 * 
 * @author <a href="mailto:andrew.uljanenko@gmail.com">Andrew Uljanenko</a>
 */
public class NonAccessibleTypeCheck extends Check
{
    /**
     * Key for error message.
     */
    public static final String MSG_KEY = "non.accessible.type";

    /**
     * Regular expression with specified types.
     */
    private Pattern nonAccessibleTypes;

    /**
     * List of on-demand-imports for current AST.
     */
    private List<String> onDemandImports = new ArrayList<String>();

    /**
     * List of single-type-imports for current AST.
     */
    private List<String> singleTypeImports = new ArrayList<String>();

    /**
     * Name of current package.
     */
    private String packageName;

    /**
     * Initialization of variable NonAccessibleTypes.
     * @param restRictedTypes
     *        string with types.
     */
    public void setNonAccessibleTypes(final String restrictedTypes)
    {
        if (restrictedTypes != null && !restrictedTypes.isEmpty()) {
            nonAccessibleTypes = Pattern.compile(restrictedTypes);
        }
        else {
            nonAccessibleTypes = null;
        }
    }
    
    @Override
    public final int[] getDefaultTokens()
    {
        return new int[] {
                TokenTypes.PACKAGE_DEF,
                TokenTypes.IMPORT,
                TokenTypes.VARIABLE_DEF,
                TokenTypes.METHOD_DEF,
        };
    }
    
    @Override
    public void beginTree(DetailAST node)
    {
        onDemandImports.clear();
        singleTypeImports.clear();
        packageName = "";
    }
    
    @Override
    public void visitToken(DetailAST node)
    {
        switch (node.getType()) {
            case TokenTypes.PACKAGE_DEF:
            {
                packageName = getIdentifierName(node);
            }
                break;
            case TokenTypes.IMPORT:
            {
                String currentImport = getIdentifierName(node);
                if (isOnDemandImport(currentImport)) {
                    onDemandImports.add(currentImport);
                }
                else {
                    singleTypeImports.add(currentImport);
                }
            }
                break;
            case TokenTypes.VARIABLE_DEF:
            {
                List<String> memberTypes = collectMemberTypes(node);
                if (!isPrivateMember(node) && canAccessFromOutside(node)) {
                    for (String type : memberTypes) {
                        if (isNonAccessibleType(type)) {
                            log(node.getLineNo(), MSG_KEY, type);
                        }
                    }
                }
            }
                break;
            case TokenTypes.METHOD_DEF:
            {
                List<String> memberTypes = collectMemberTypes(node);
                if (!isPrivateMember(node) && !hasOverrideAnnotation(node)) {
                    for (String type : memberTypes) {
                        if (isNonAccessibleType(type)) {
                            log(node.getLineNo(), MSG_KEY, type);
                        }
                    }
                }
            }
                break;
        }
    }
        
    /**
     * Returns full name of identifier.
     * @param identifierNode
     *        AST node.
     * @return string with full name of current identifier.
     */
    private String getIdentifierName(DetailAST identifierNode)
    {
        DetailAST identNode = identifierNode.findFirstToken(TokenTypes.IDENT);
        DetailAST dotNode = identifierNode.findFirstToken(TokenTypes.DOT);
        String result;

        if (identNode != null) {
            result = identNode.getText();
        }
        else if (dotNode != null) {
            StringBuilder builder = new StringBuilder(40);
            while (dotNode.getType() == TokenTypes.DOT) {
                builder.insert(0, '.').insert(1, dotNode.getLastChild().getText());
                dotNode = dotNode.getFirstChild();
            }
            builder.insert(0, dotNode.getText());
            result = builder.toString();
        }
        else {
            result = identifierNode.getFirstChild().getText();
        }
        return result;
    }

    /**
     * Checks whether import is on demand import.
     * @param importName
     *        name of import.
     * @return true, if import is on-demand import.
     */
    private static boolean isOnDemandImport(String importName)
    {
        return importName.endsWith(".*");
    }

    /**
     * Collects all member types. If there is generic type, then method will return all of types.
     * @param definitionNode
     *        AST definition node.
     * @return list, which contains all member types.
     */
    private List<String> collectMemberTypes(DetailAST definitionNode)
    {
        List<String> memberTypes = new ArrayList<String>();
        DetailAST typeNode = definitionNode.findFirstToken(TokenTypes.TYPE);

        if (typeNode.getChildCount() == 1) {
            memberTypes.add(getIdentifierName(typeNode));
        }
        else {
            DetailAST typeArgumentsNode = typeNode.findFirstToken(TokenTypes.TYPE_ARGUMENTS)
                    .getFirstChild();
            while (typeArgumentsNode.getNextSibling() != null) {
                if (typeArgumentsNode.getType() == TokenTypes.TYPE_ARGUMENT) {
                    memberTypes.add(getIdentifierName(typeArgumentsNode));
                }
                typeArgumentsNode = typeArgumentsNode.getNextSibling();
            }
        }
        return memberTypes;
    }

    /**
     * Checks whether a member of a private.
     * @param definitionNode
     *        AST definition node.
     * @return true, if member is private.
     */
    private boolean isPrivateMember(DetailAST definitionNode)
    {
        boolean result = false;
        DetailAST modifiersNode = definitionNode.findFirstToken(TokenTypes.MODIFIERS);
        if (modifiersNode != null &&
                modifiersNode.branchContains(TokenTypes.LITERAL_PRIVATE)) {
            result = true;
        }
        return result;
    }

    /**
     * Checks matches current type regular expression.
     * @param type
     *        String with type.
     * @return true, if current type matches with regexp.
     */
    private boolean isNonAccessibleType(String type)
    {
        if (nonAccessibleTypes != null) {
            //if qualified type
            if (nonAccessibleTypes.matcher(type).matches()) {
                return true;
            }
            else {
                //try to join type with on-demand-imports
                for (String currentOnDemandImport : onDemandImports) {
                    String fullType =
                            joinOnDemandImportWithIdentifier(currentOnDemandImport, type);
                    if (nonAccessibleTypes.matcher(fullType).matches()) {
                        return true;
                    }
                }
                //try to join type with single-type-imports
                for (String currentSingleTypeImport : singleTypeImports) {
                    String importEntryLastPart = currentSingleTypeImport.
                            substring(currentSingleTypeImport.lastIndexOf(".") + 1);
                    if (importEntryLastPart.equals(type)
                            && nonAccessibleTypes.matcher(currentSingleTypeImport).matches()) {
                        return true;
                    }
                }
                //if the type described in current package
                String fullNameWithPackage = packageName + "." + type;
                if (nonAccessibleTypes.matcher(fullNameWithPackage).matches()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Joins on demand import entry and identifier name into fully qualified name.
     * @param importEntry
     *        on demand import entry for join.
     * @param identifierName
     *        identifier name to join to import.
     * @return fully qualified identifier name.
     */
    private static String
            joinOnDemandImportWithIdentifier(String importEntry, String identifierName)
    {
        return importEntry.substring(0, importEntry.length() - 1) + identifierName;
    }

    /**
     * Checks for @Override annotation.
     * @param definitionNode
     *        AST method definition node.
     * @return true, if method has @Override annotation.
     */
    private static boolean hasOverrideAnnotation(DetailAST definitionNode)
    {
        boolean result = false;
        DetailAST annotationNode = definitionNode.findFirstToken(TokenTypes.ANNOTATION);
        if (annotationNode != null
                && annotationNode.findFirstToken(TokenTypes.IDENT).getText().equals("Override")) {
            result = true;
        }
        return result;
    }

    /**
     * Checks where there is a field(in class, enum or interface), and whether you can get access to
     * it.
     * @param defNode
     *        AST field definition node.
     * @return true, if field is not private and accessible from outside.
     */
    private boolean canAccessFromOutside(DetailAST fieldDefNode)
    {
        boolean result = false;
        DetailAST definitionNode = fieldDefNode.getParent().getParent();
        if ((definitionNode.getType() == TokenTypes.CLASS_DEF
                && !isClassDefInMethodDef(definitionNode)
                && isAccessibleFromOutsideClass(definitionNode))
                || definitionNode.getType() == TokenTypes.ENUM_DEF
                || definitionNode.getType() == TokenTypes.INTERFACE_DEF) {
            result = true;
        }
        return result;
    }

    /**
     * Checks nesting of classes and the ability to access them.
     * @param definitionNode
     *        AST definition node.
     * @return true, if external classes are not private.
     */
    private boolean isAccessibleFromOutsideClass(DetailAST definitionNode)
    {
        boolean result = true;
        DetailAST currentNode = definitionNode;
        while (currentNode != null) {
            if (isPrivateMember(currentNode)) {
                result = false;
                break;
            }
            currentNode = currentNode.getParent();
        }
        return result;
    }

    /**
     * Verify that class definition is in method definition.
     * @param classDefNode
     *        AST definition node.
     * @return true, if class definition is in method definition.
     */
    private boolean isClassDefInMethodDef(DetailAST classDefNode)
    {
        boolean result = false;
        DetailAST currentParentNode = classDefNode.getParent();
        while (currentParentNode != null) {
            if (currentParentNode.getType() == TokenTypes.METHOD_DEF) {
                result = true;
                break;
            }
            currentParentNode = currentParentNode.getParent();
        }
        return result;
    }
}
