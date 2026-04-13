package com.oikoaudio.fire.values;

import com.bitwig.extension.controller.api.Clip;
import com.bitwig.extensions.framework.values.BooleanValueObject;
import com.bitwig.extensions.framework.values.IntValueObject;

public class StepViewPosition {

    private double gridResolution;
    private double loopLength = 0.0;
    private int pagePosition = 0;
    private int pages = 0;
    private int steps;
    private int stepsPerPage;

    private final Clip clip;
    private final BooleanValueObject canScrollLeft = new BooleanValueObject();
    private final BooleanValueObject canScrollRight = new BooleanValueObject();
    private final String name;
    private final IntValueObject stepsValue = new IntValueObject(0, 0, 2048);

    public StepViewPosition(final Clip clip, final int stepsPerPage, final String name) {
        this.clip = clip;
        this.gridResolution = 0.25;
        this.name = name;
        this.stepsPerPage = stepsPerPage;
        this.clip.setStepSize(gridResolution);
        clip.getLoopLength().addValueObserver(this::handleLoopLengthChanged);
        clip.scrollToStep(pagePosition * stepsPerPage);
    }

    public double lengthWithLastStep(final int index) {
        return gridResolution * (pagePosition * 32 + index + 1);
    }

    public void setStepsPerPage(final int stepsPerPage) {
        this.stepsPerPage = stepsPerPage;
        steps = (int) (this.loopLength / gridResolution);
        stepsValue.set(steps);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        updateStates();
    }

    public void handleLoopLengthChanged(final double newLength) {
        this.loopLength = newLength;
        steps = (int) (this.loopLength / gridResolution);
        stepsValue.set(steps);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        updateStates();
    }

    public IntValueObject getStepsValue() {
        return stepsValue;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(final int index) {
        steps = index + 1;
        clip.getLoopLength().set(steps * gridResolution);
    }

    public int getCurrentPage() {
        return pagePosition;
    }

    public int getAvailableSteps() {
        return Math.min(stepsPerPage, Math.max(0, steps - pagePosition * stepsPerPage));
    }

    public int getPages() {
        return pages;
    }

    private void updateStates() {
        pagePosition = Math.max(0, Math.min(pagePosition, Math.max(0, pages - 1)));
        clip.scrollToStep(pagePosition * stepsPerPage);
        canScrollLeft.set(pagePosition > 0);
        canScrollRight.set(pagePosition < pages - 1);
    }

    public BooleanValueObject canScrollLeft() {
        return canScrollLeft;
    }

    public BooleanValueObject canScrollRight() {
        return canScrollRight;
    }

    public void setPage(final int index) {
        this.pagePosition = Math.max(0, Math.min(index, Math.max(0, pages - 1)));
        updateStates();
    }

    public int getStepOffset() {
        return pagePosition * stepsPerPage;
    }

    public double getPosition() {
        return pagePosition * gridResolution;
    }

    public double getGridResolution() {
        return gridResolution;
    }

    public void setGridResolution(final double resolution) {
        final double quote = this.gridResolution / resolution;
        gridResolution = resolution;
        this.clip.setStepSize(gridResolution);
        pagePosition = (int) (pagePosition * quote);
        steps = (int) (this.loopLength / gridResolution);
        stepsValue.set(steps);
        pages = Math.max(0, steps - 1) / stepsPerPage + 1;
        clip.scrollToStep(pagePosition * stepsPerPage);

        updateStates();
    }

    public double getLoopLength() {
        return loopLength;
    }

    public void setLoopLength(final double loopLength) {
        this.loopLength = loopLength;
    }

    public void scrollLeft() {
        if (pagePosition > 0) {
            pagePosition--;
            clip.scrollToStep(pagePosition * stepsPerPage);
            updateStates();
        }
    }

    public void scrollRight() {
        if (pagePosition < pages - 1) {
            pagePosition++;
            clip.scrollToStep(pagePosition * stepsPerPage);
            updateStates();
        }
    }
}
