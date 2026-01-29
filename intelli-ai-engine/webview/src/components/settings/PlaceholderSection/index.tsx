import {useTranslation} from 'react-i18next';
import styles from './style.module.less';

interface PlaceholderSectionProps {
  type: 'permissions';
}

const PlaceholderSection = ({ type }: PlaceholderSectionProps) => {
  const { t } = useTranslation();

  const sectionConfig = {
    permissions: {
      title: t('settings.permissions'),
      desc: t('settings.permissionsDesc'),
      icon: 'codicon-shield',
      message: t('settings.permissionsComingSoon'),
    },
  };

  const config = sectionConfig[type];

  return (
    <div className={styles.configSection}>
      <h3 className={styles.sectionTitle}>{config.title}</h3>
      <p className={styles.sectionDesc}>{config.desc}</p>
      <div className={styles.tempNotice}>
        <span className={`codicon ${config.icon}`} />
        <p>{config.message}</p>
      </div>
    </div>
  );
};

export default PlaceholderSection;
