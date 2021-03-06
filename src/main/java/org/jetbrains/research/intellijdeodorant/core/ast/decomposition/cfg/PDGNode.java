package org.jetbrains.research.intellijdeodorant.core.ast.decomposition.cfg;

import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import org.jetbrains.research.intellijdeodorant.core.ast.*;
import org.jetbrains.research.intellijdeodorant.core.ast.decomposition.AbstractStatement;
import org.jetbrains.research.intellijdeodorant.core.ast.util.ExpressionExtractor;

import java.util.*;

import static org.jetbrains.research.intellijdeodorant.utils.PsiUtils.isPrimitive;

public class PDGNode extends GraphNode implements Comparable<PDGNode> {
    private CFGNode cfgNode;
    final Set<AbstractVariable> declaredVariables;
    protected final Set<AbstractVariable> definedVariables;
    protected final Set<AbstractVariable> usedVariables;
    final Set<CreationObject> createdTypes;
    final Set<String> thrownExceptionTypes;
    private Set<VariableDeclarationObject> variableDeclarationsInMethod;
    private Set<FieldObject> fieldsAccessedInMethod;
    private Set<AbstractVariable> originalDefinedVariables;
    private Set<AbstractVariable> originalUsedVariables;

    PDGNode() {
        super();
        this.declaredVariables = new LinkedHashSet<>();
        this.definedVariables = new LinkedHashSet<>();
        this.usedVariables = new LinkedHashSet<>();
        this.createdTypes = new LinkedHashSet<>();
        this.thrownExceptionTypes = new LinkedHashSet<>();
    }

    PDGNode(CFGNode cfgNode, Set<VariableDeclarationObject> variableDeclarationsInMethod,
            Set<FieldObject> fieldsAccessedInMethod) {
        super();
        this.cfgNode = cfgNode;
        this.variableDeclarationsInMethod = variableDeclarationsInMethod;
        this.fieldsAccessedInMethod = fieldsAccessedInMethod;
        this.id = cfgNode.id;
        cfgNode.setPDGNode(this);
        this.declaredVariables = new LinkedHashSet<>();
        this.definedVariables = new LinkedHashSet<>();
        this.usedVariables = new LinkedHashSet<>();
        this.createdTypes = new LinkedHashSet<>();
        this.thrownExceptionTypes = new LinkedHashSet<>();
    }

    public CFGNode getCFGNode() {
        return cfgNode;
    }

    public PDGNode getControlDependenceParent() {
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                return (PDGNode) dependence.src;
            }
        }
        return null;
    }

    boolean hasIncomingControlDependenceFromMethodEntryNode() {
        for (GraphEdge edge : incomingEdges) {
            PDGDependence dependence = (PDGDependence) edge;
            if (dependence instanceof PDGControlDependence) {
                PDGNode srcNode = (PDGNode) dependence.src;
                if (srcNode instanceof PDGMethodEntryNode)
                    return true;
            }
        }
        return false;
    }

    boolean declaresLocalVariable(AbstractVariable variable) {
        return declaredVariables.contains(variable);
    }

    boolean definesLocalVariable(AbstractVariable variable) {
        return definedVariables.contains(variable);
    }

    boolean usesLocalVariable(AbstractVariable variable) {
        return usedVariables.contains(variable);
    }

    boolean instantiatesLocalVariable(AbstractVariable variable) {
        if (variable instanceof PlainVariable && this.definesLocalVariable(variable)) {
            PlainVariable plainVariable = (PlainVariable) variable;
            String variableType = plainVariable.getType();
            for (CreationObject creation : createdTypes) {
                if (creation instanceof ClassInstanceCreationObject) {
                    PsiNewExpression classInstanceCreationExpression =
                            ((ClassInstanceCreationObject) creation).getClassInstanceCreation();
                    PsiReference psiReference = classInstanceCreationExpression.getReference();
                    if (psiReference != null) {
                        PsiClass referencedClass = (PsiClass) psiReference.getElement();
                        PsiClass superClass = referencedClass.getSuperClass();
                        Set<String> implementedInterfaces = new LinkedHashSet<>();
                        if (superClass != null && superClass.getInterfaces().length > 0) {
                            for (PsiClass implementedInterface : superClass.getInterfaces()) {
                                implementedInterfaces.add(implementedInterface.getName());
                            }
                        }
                        if (variableType.equals(referencedClass.getQualifiedName())
                                || variableType.equals(Objects.requireNonNull(superClass).getQualifiedName())
                                || implementedInterfaces.contains(variableType))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    boolean containsClassInstanceCreation() {
        return !createdTypes.isEmpty();
    }

    boolean throwsException() {
        return !thrownExceptionTypes.isEmpty();
    }

    public BasicBlock getBasicBlock() {
        return cfgNode.getBasicBlock();
    }

    public AbstractStatement getStatement() {
        return cfgNode.getStatement();
    }

    public PsiStatement getASTStatement() {
        return cfgNode.getASTStatement();
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof PDGNode) {
            PDGNode pdgNode = (PDGNode) o;
            return this.cfgNode.equals(pdgNode.cfgNode);
        }
        return false;
    }

    public int hashCode() {
        return cfgNode.hashCode();
    }

    public String toString() {
        return cfgNode.toString();
    }

    public int compareTo(PDGNode node) {
        return Integer.compare(this.getId(), node.getId());
    }

    void updateReachingAliasSet(ReachingAliasSet reachingAliasSet) {
        Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<>();
        variableDeclarations.addAll(variableDeclarationsInMethod);
        variableDeclarations.addAll(fieldsAccessedInMethod);
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = declarationStatement.getDeclaredElements();
            if (declaredElements.length > 0 && declaredElements[0] instanceof PsiVariable) {
                PsiVariable declaredVariable = (PsiVariable) declaredElements[0];
                if (!isPrimitive(declaredVariable.getType())) {
                    PsiExpression initializer = declaredVariable.getInitializer();
                    PsiElement initializerSimpleName = null;
                    if (initializer != null) {
                        if (initializer instanceof PsiReferenceExpression) {
                            initializerSimpleName = ((PsiReferenceExpression) initializer).resolve();
                        }
                        if (initializerSimpleName != null) {
                            PsiVariable initializerVariableDeclaration = null;
                            for (VariableDeclarationObject declarationObject : variableDeclarations) {
                                PsiVariable declaration = declarationObject.getVariableDeclaration();
                                if (declaration.equals((initializerSimpleName))) {
                                    initializerVariableDeclaration = declaration;
                                    break;
                                }
                            }
                            if (initializerVariableDeclaration != null) {
                                reachingAliasSet.insertAlias(declaredVariable, initializerVariableDeclaration);
                            }
                        }
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            if (expression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;
                processAssignment(reachingAliasSet, variableDeclarations, assignment);
            }
        }
    }

    private void processAssignment(ReachingAliasSet reachingAliasSet,
                                   Set<VariableDeclarationObject> variableDeclarations, PsiAssignmentExpression assignment) {
        PsiExpression leftHandSideExpression = assignment.getLExpression();
        PsiExpression rightHandSideExpression = assignment.getRExpression();
        PsiElement leftHandSideElement = null;
        if (leftHandSideExpression instanceof PsiReferenceExpression) {
            leftHandSideElement = ((PsiReferenceExpression) leftHandSideExpression).resolve();
        }
        PsiVariable leftHandSideSimpleName = null;
        if (leftHandSideElement instanceof PsiVariable) {
            leftHandSideSimpleName = (PsiVariable) leftHandSideElement;
        }
        if (leftHandSideSimpleName != null && !isPrimitive(leftHandSideSimpleName.getType())) {
            PsiVariable leftHandSideVariableDeclaration = null;
            for (VariableDeclarationObject declarationObject : variableDeclarations) {
                PsiVariable declaration = declarationObject.getVariableDeclaration();
                if (declaration.equals(leftHandSideSimpleName)) {
                    leftHandSideVariableDeclaration = declaration;
                    break;
                }
            }
            PsiElement rightHandSideSimpleName = null;
            if (rightHandSideExpression instanceof PsiReferenceExpression) {
                rightHandSideSimpleName = ((PsiReferenceExpression) rightHandSideExpression).resolve();
            } else if (rightHandSideExpression instanceof PsiAssignmentExpression) {
                PsiAssignmentExpression rightHandSideAssignment = (PsiAssignmentExpression) rightHandSideExpression;
                processAssignment(reachingAliasSet, variableDeclarations, rightHandSideAssignment);
                PsiExpression leftHandSideExpressionOfRightHandSideAssignment = rightHandSideAssignment.getLExpression();
                PsiElement leftHandSideSimpleNameOfRightHandSideAssignment = null;
                if (leftHandSideExpressionOfRightHandSideAssignment instanceof PsiReferenceExpression) {
                    leftHandSideSimpleNameOfRightHandSideAssignment = leftHandSideExpressionOfRightHandSideAssignment;
                } else if (leftHandSideExpressionOfRightHandSideAssignment instanceof PsiVariable) {
                    leftHandSideSimpleNameOfRightHandSideAssignment = leftHandSideExpressionOfRightHandSideAssignment;
                }
                if (leftHandSideSimpleNameOfRightHandSideAssignment != null) {
                    rightHandSideSimpleName = leftHandSideSimpleNameOfRightHandSideAssignment;
                }
            }
            if (rightHandSideSimpleName != null) {
                PsiVariable rightHandSideVariableDeclaration = null;
                for (VariableDeclarationObject declarationObject : variableDeclarations) {
                    PsiVariable declaration = declarationObject.getVariableDeclaration();
                    if (declaration.equals(rightHandSideSimpleName)) {
                        rightHandSideVariableDeclaration = declaration;
                        break;
                    }
                }
                if (leftHandSideVariableDeclaration != null && rightHandSideVariableDeclaration != null) {
                    reachingAliasSet.insertAlias(leftHandSideVariableDeclaration, rightHandSideVariableDeclaration);
                }
            } else {
                if (leftHandSideVariableDeclaration != null) {
                    reachingAliasSet.removeAlias(leftHandSideVariableDeclaration);
                }
            }
        }
    }

    void applyReachingAliasSet(ReachingAliasSet reachingAliasSet) {
        if (originalDefinedVariables == null)
            originalDefinedVariables = new LinkedHashSet<>(definedVariables);
        Set<AbstractVariable> defVariablesToBeAdded = new LinkedHashSet<>();
        for (AbstractVariable abstractVariable : originalDefinedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (reachingAliasSet.containsAlias(compositeVariable)) {
                    Set<PsiVariable> aliases = reachingAliasSet.getAliases(compositeVariable);
                    for (PsiVariable alias : aliases) {
                        CompositeVariable aliasCompositeVariable =
                                new CompositeVariable(alias, compositeVariable.getRightPart());
                        defVariablesToBeAdded.add(aliasCompositeVariable);
                    }
                }
            }
        }
        definedVariables.addAll(defVariablesToBeAdded);
        if (originalUsedVariables == null)
            originalUsedVariables = new LinkedHashSet<>(usedVariables);
        Set<AbstractVariable> useVariablesToBeAdded = new LinkedHashSet<>();
        for (AbstractVariable abstractVariable : originalUsedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (reachingAliasSet.containsAlias(compositeVariable)) {
                    Set<PsiVariable> aliases = reachingAliasSet.getAliases(compositeVariable);
                    for (PsiVariable alias : aliases) {
                        CompositeVariable aliasCompositeVariable = new CompositeVariable(alias, compositeVariable.getRightPart());
                        useVariablesToBeAdded.add(aliasCompositeVariable);
                    }
                }
            }
        }
        usedVariables.addAll(useVariablesToBeAdded);
    }

    Map<PsiVariable, PsiNewExpression> getClassInstantiations() {
        Map<PsiVariable, PsiNewExpression> classInstantiationMap = new LinkedHashMap<>();
        Set<VariableDeclarationObject> variableDeclarations = new LinkedHashSet<>();
        variableDeclarations.addAll(variableDeclarationsInMethod);
        variableDeclarations.addAll(fieldsAccessedInMethod);
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement vDStatement = (PsiDeclarationStatement) statement;
            PsiElement[] declaredElements = vDStatement.getDeclaredElements();
            for (PsiElement psiElement : declaredElements) {
                if (psiElement instanceof PsiVariable) {
                    PsiVariable psiVariable = (PsiVariable) psiElement;
                    PsiExpression psiExpression = psiVariable.getInitializer();
                    if (psiExpression instanceof PsiNewExpression) {
                        PsiNewExpression classInstanceCreation = (PsiNewExpression) psiExpression;
                        classInstantiationMap.put(psiVariable, classInstanceCreation);
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
            for (PsiExpression assignmentExpression : assignments) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) assignmentExpression;
                PsiExpression leftHandSideExpression = assignment.getLExpression();
                PsiExpression rightHandSideExpression = assignment.getRExpression();
                if (rightHandSideExpression instanceof PsiNewExpression) {
                    PsiNewExpression classInstanceCreation = (PsiNewExpression) rightHandSideExpression;
                    PsiElement leftHandSideSimpleName = null;
                    if (((PsiReferenceExpressionImpl) leftHandSideExpression).resolve() instanceof PsiVariable) {
                        leftHandSideSimpleName = ((PsiReferenceExpressionImpl) leftHandSideExpression).resolve();
                    }
                    if (leftHandSideSimpleName != null) {
                        PsiVariable leftHandSideVariableDeclaration = null;
                        for (VariableDeclarationObject declarationObject : variableDeclarations) {
                            PsiVariable declaration = declarationObject.getVariableDeclaration();
                            if (declaration.equals(leftHandSideSimpleName)) {
                                leftHandSideVariableDeclaration = declaration;
                                break;
                            }
                        }
                        if (leftHandSideVariableDeclaration != null) {
                            classInstantiationMap.put(leftHandSideVariableDeclaration, classInstanceCreation);
                        }
                    }
                }
            }
        }
        return classInstantiationMap;
    }

    boolean changesStateOfReference(PsiVariable variableDeclaration) {
        for (AbstractVariable abstractVariable : definedVariables) {
            if (abstractVariable instanceof CompositeVariable) {
                CompositeVariable compositeVariable = (CompositeVariable) abstractVariable;
                if (variableDeclaration.equals(compositeVariable.getOrigin()))
                    return true;
            }
        }
        return false;
    }

    boolean accessesReference(PsiVariable variableDeclaration) {
        for (AbstractVariable abstractVariable : usedVariables) {
            if (abstractVariable instanceof PlainVariable) {
                PlainVariable plainVariable = (PlainVariable) abstractVariable;
                if (variableDeclaration.equals(plainVariable.getOrigin()))
                    return true;
            }
        }
        return false;
    }

    boolean assignsReference(PsiVariable variableDeclaration) {
        PsiElement statement = getASTStatement();
        if (statement instanceof PsiDeclarationStatement) {
            PsiDeclarationStatement vDStatement = (PsiDeclarationStatement) statement;
            PsiElement[] psiElements = vDStatement.getDeclaredElements();
            for (PsiElement psiElement : psiElements) {
                if (psiElement instanceof PsiVariable) {
                    PsiVariable psiVariable = (PsiVariable) psiElement;
                    PsiExpression initializer = psiVariable.getInitializer();
                    PsiElement initializerSimpleName = null;
                    if (initializer instanceof PsiReferenceExpression) {
                        PsiElement element = ((PsiReferenceExpression) initializer).resolve();
                        if (element instanceof PsiVariable) {
                            initializerSimpleName = element;
                        }
                    }
                    if (initializerSimpleName != null) {
                        if (variableDeclaration.equals(initializerSimpleName)) {
                            return true;
                        }
                    }
                }
            }
        } else if (statement instanceof PsiExpressionStatement) {
            PsiExpressionStatement expressionStatement = (PsiExpressionStatement) statement;
            PsiExpression expression = expressionStatement.getExpression();
            ExpressionExtractor expressionExtractor = new ExpressionExtractor();
            List<PsiExpression> assignments = expressionExtractor.getAssignments(expression);
            for (PsiExpression assignmentExpression : assignments) {
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) assignmentExpression;
                PsiExpression rightHandSideExpression = assignment.getRExpression();
                PsiElement rightHandSideSimpleName = null;
                if (rightHandSideExpression instanceof PsiReferenceExpression) {
                    rightHandSideSimpleName = ((PsiReferenceExpression) rightHandSideExpression).resolve();
                }
                if (rightHandSideSimpleName != null) {
                    if (variableDeclaration.equals(rightHandSideSimpleName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
