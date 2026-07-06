// Stepper indicator component

interface StepperProps {
  step: number;
}

export default function Stepper({ step }: StepperProps) {
  if (step < 2 || step > 6) return null;
  
  return (
    <div 
      className="stepper" 
      aria-label="見積プロセス進捗" 
      role="progressbar" 
      aria-valuenow={step - 1} 
      aria-valuemin={1} 
      aria-valuemax={5}
    >
      {[2, 3, 4, 5, 6].map((num) => {
        const isCurrent = step === num;
        const isCompleted = step > num;
        
        let statusClass = 'step-node';
        if (isCurrent) statusClass += ' active';
        else if (isCompleted) statusClass += ' completed';
        
        let stepLabel = '';
        let stepDesc = '';
        
        switch (num) {
          case 2:
            stepLabel = '人';
            stepDesc = 'お客様情報の入力';
            break;
          case 3:
            stepLabel = '険';
            stepDesc = '現在加入中の保険確認';
            break;
          case 4:
            stepLabel = '車';
            stepDesc = 'お車の情報入力';
            break;
          case 5:
            stepLabel = '補';
            stepDesc = '補償条件と特約の設定';
            break;
          case 6:
            stepLabel = '確';
            stepDesc = '入力情報の確認';
            break;
          default:
            break;
        }

        return (
          <div 
            key={num} 
            className={statusClass} 
            title={stepDesc}
            aria-current={isCurrent ? 'step' : undefined}
          >
            <span>{stepLabel}</span>
          </div>
        );
      })}
    </div>
  );
}
