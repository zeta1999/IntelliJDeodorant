package gr.uom.java.ast;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

public class FieldInstructionObject {

    private final String ownerClass;
    private final TypeObject type;
    private final String name;
    private boolean _static;
    private ASTInformation simpleName;
    private volatile int hashCode = 0;
    private PsiReference variableBindingKey;

    public FieldInstructionObject(String ownerClass, TypeObject type, String name) {
        this.ownerClass = ownerClass;
        this.type = type;
        this.name = name;
        this._static = false;
    }

    public FieldInstructionObject(String ownerClass, TypeObject type, String name, PsiReference variableBindingKey) {
        this(ownerClass, type, name);
        this.variableBindingKey = variableBindingKey;
    }

    public String getOwnerClass() {
        return ownerClass;
    }

    public TypeObject getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public PsiReference getVariableBindingKey() {
        return variableBindingKey;
    }

    public boolean isStatic() {
        return _static;
    }

    public void setStatic(boolean s) {
        _static = s;
    }

    public void setSimpleName(PsiElement simpleName) {
        //this.simpleName = simpleName;
        this.variableBindingKey = simpleName.getReference();
        this.simpleName = ASTInformationGenerator.generateASTInformation(simpleName);
    }

    public PsiElement getSimpleName() {
        //return this.simpleName;
        return this.simpleName.recoverASTNode();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof FieldInstructionObject) {
            FieldInstructionObject fio = (FieldInstructionObject) o;
            return this.ownerClass.equals(fio.ownerClass) && this.name.equals(fio.name) && this.type.equals(fio.type) &&
                    this.variableBindingKey.equals(fio.variableBindingKey);
        }
        return false;
    }

    public int hashCode() {
        if (hashCode == 0) {
            int result = 17;
            result = 37 * result + ownerClass.hashCode();
            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();
          //  result = 37 * result + variableBindingKey.hashCode();
            hashCode = result;
        }
        return hashCode;
    }

    public String toString() {
        return ownerClass + "::" + name;
    }
}