package com.simisinc.platform.domain.model.dashboard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProgressCardTest {
  @Test
  void testGetPercentComplete() {
    ProgressCard progressCard = new ProgressCard();
    progressCard.setProgress(50);
    progressCard.setMaxValue(100);
    Assertions.assertEquals("50.00%", progressCard.getPercentComplete());

    progressCard.setProgress(30);
    progressCard.setMaxValue(90);
    Assertions.assertEquals("33.33%", progressCard.getPercentComplete());

    progressCard.setProgress(3);
    progressCard.setMaxValue(7);
    Assertions.assertEquals("42.86%", progressCard.getPercentComplete());
  }
}
