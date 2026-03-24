package edu.tcu.cs.hogwartsartifactsonline.artifact.dto;

import edu.tcu.cs.hogwartsartifactsonline.wizard.dto.WizardDto;

public record ArtifactDto(String id, String name, String description, String imageUrl, WizardDto owner) {

}
