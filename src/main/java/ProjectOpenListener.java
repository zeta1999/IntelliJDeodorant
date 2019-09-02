import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import gr.uom.java.distance.ProjectInfo;

class ProjectOpenListener implements ProjectComponent {
    private final Project project;
    private ProjectInfo projectInfo;

    public ProjectOpenListener(Project project) {
        this.project = project;
        this.projectInfo = new ProjectInfo(project);
    }

    @Override
    public void projectOpened() {
        final DumbService dumbService = DumbService.getInstance(project);
        dumbService.runWhenSmart(() -> {
/*          this.projectInfo = new ProjectInfo(project);
            new ASTReader(projectInfo);
            List<MoveMethodCandidateRefactoring> candidates = Standalone.getMoveMethodRefactoringOpportunities(projectInfo);*/
        });

    }

    @Override
    public void projectClosed() {

    }
}