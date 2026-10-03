CREATE UNIQUE INDEX UX_Adjustment_Reversal ON dbo.Adjustment(originalAdjustmentId) WHERE originalAdjustmentId IS NOT NULL;
GO
